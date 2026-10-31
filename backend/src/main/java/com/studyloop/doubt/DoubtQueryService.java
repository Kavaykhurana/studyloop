package com.studyloop.doubt;

import com.studyloop.answer.Answer;
import com.studyloop.answer.AnswerMapper;
import com.studyloop.answer.AnswerRepository;
import com.studyloop.answer.dto.AnswerDto;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.common.web.PageResponse;
import com.studyloop.doubt.dto.DoubtDetail;
import com.studyloop.doubt.dto.DoubtSummary;
import com.studyloop.hint.HintDao;
import com.studyloop.hint.dto.HintDto;
import com.studyloop.matching.MatchStore;
import com.studyloop.matching.dto.MatchDto;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.user.UserLookup;
import com.studyloop.user.dto.UserBrief;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoubtQueryService {

    public record FeedFilter(DoubtStatus status, Long subjectId, Long topicId, boolean mine, boolean matchedToMe,
                             String q) {}

    private final DoubtRepository doubts;
    private final AnswerRepository answers;
    private final AnswerMapper answerMapper;
    private final DoubtMapper doubtMapper;
    private final MatchStore matchStore;
    private final HintDao hintDao;
    private final UserLookup userLookup;
    private final SubjectCatalog catalog;

    public DoubtQueryService(DoubtRepository doubts, AnswerRepository answers, AnswerMapper answerMapper,
                             DoubtMapper doubtMapper, MatchStore matchStore, HintDao hintDao, UserLookup userLookup,
                             SubjectCatalog catalog) {
        this.doubts = doubts;
        this.answers = answers;
        this.answerMapper = answerMapper;
        this.doubtMapper = doubtMapper;
        this.matchStore = matchStore;
        this.hintDao = hintDao;
        this.userLookup = userLookup;
        this.catalog = catalog;
    }

    /** Feed: one page query, one count query and one batched author lookup, regardless of page size. */
    @Transactional(readOnly = true)
    public PageResponse<DoubtSummary> feed(CurrentUser me, FeedFilter filter, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        List<Long> matchedIds = filter.matchedToMe() ? matchStore.openMatchedDoubtIds(me.userId(), me.campusId())
                : List.of();
        if (filter.matchedToMe() && matchedIds.isEmpty()) {
            return PageResponse.of(List.of(), pageable.getPageNumber(), safeSize, 0);
        }
        Specification<Doubt> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("campusId"), me.campusId()));
            if (filter.status() != null) {
                p.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.subjectId() != null) {
                p.add(cb.equal(root.get("subjectId"), filter.subjectId()));
            }
            if (filter.topicId() != null) {
                p.add(cb.equal(root.get("topicId"), filter.topicId()));
            }
            if (filter.mine()) {
                p.add(cb.equal(root.get("authorId"), me.userId()));
            }
            if (filter.matchedToMe()) {
                p.add(root.get("id").in(matchedIds));
            }
            if (filter.q() != null) {
                String pattern = "%" + escapeLike(filter.q().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("bodyMd")), pattern, '\\')));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Page<Doubt> result = doubts.findAll(spec, pageable);
        List<DoubtSummary> items = doubtMapper.summaries(result.getContent());
        return PageResponse.of(items, result.getNumber(), safeSize, result.getTotalElements());
    }

    @Transactional
    public DoubtDetail detail(CurrentUser me, long id, boolean countView) {
        Doubt d = doubts.findByIdAndCampusId(id, me.campusId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        boolean isAuthor = d.getAuthorId().equals(me.userId());
        int views = d.getViewCount();
        if (countView && !isAuthor) {
            doubts.incrementViews(id);
            views++;
        }
        UserBrief author = userLookup.briefs(List.of(d.getAuthorId())).get(d.getAuthorId());
        List<Answer> answerRows = answers.findByDoubtIdOrderByCreatedAtAscIdAsc(id);
        List<AnswerDto> answerDtos = answerMapper.toDtos(answerRows, me.userId());
        List<MatchDto> matches = matchStore.find(id);
        int unlocked = hintDao.levelUnlocked(me.userId(), id);
        List<HintDto> hintDtos = hintDao.toDtos(hintDao.findUpTo(id, unlocked), me.campusId());
        boolean open = d.getStatus() == DoubtStatus.OPEN;
        DoubtDetail.Permissions permissions = new DoubtDetail.Permissions(isAuthor, !isAuthor && open,
                isAuthor && open && d.getAnswerCount() > 0, isAuthor && open && unlocked >= 1,
                isAuthor && d.getAnswerCount() == 0, isAuthor && open);
        return new DoubtDetail(d.getId(), d.getTitle(), catalog.ref(d.getSubjectId()),
                catalog.topic(d.getTopicId()).orElse(null), d.getDifficulty(), d.getStatus().name(),
                d.getResolution() == null ? null : d.getResolution().name(), d.getSource().name(), author,
                d.getAnswerCount(), d.getCreatedAt(), d.getSolvedAt(), d.getBodyMd(), views, d.getFirstAnswerAt(),
                answerDtos, matches, new DoubtDetail.Hints(unlocked, hintDtos), permissions);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
