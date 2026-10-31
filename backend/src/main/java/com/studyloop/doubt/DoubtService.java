package com.studyloop.doubt;

import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.EmbeddingClient;
import com.studyloop.ai.dto.ClassifyResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.text.ContentGuard;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.doubt.dto.CreateDoubtRequest;
import com.studyloop.doubt.dto.DoubtDetail;
import com.studyloop.doubt.dto.DoubtSummary;
import com.studyloop.embedding.EmbeddingStore;
import com.studyloop.embedding.EmbeddingText;
import com.studyloop.hint.HintDao;
import com.studyloop.matching.PeerMatchingService;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.subject.dto.TopicRef;
import com.studyloop.user.UserLookup;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoubtService {

    private static final Logger log = LoggerFactory.getLogger(DoubtService.class);

    private final DoubtRepository doubts;
    private final ClassificationService classification;
    private final EmbeddingClient embeddings;
    private final EmbeddingStore embeddingStore;
    private final PeerMatchingService matching;
    private final DoubtQueryService queries;
    private final DoubtMapper mapper;
    private final UserLookup userLookup;
    private final HintDao hintDao;
    private final SubjectCatalog catalog;
    private final RateLimiter limiter;
    private final Clock clock;

    public DoubtService(DoubtRepository doubts, ClassificationService classification, EmbeddingClient embeddings,
                        EmbeddingStore embeddingStore, PeerMatchingService matching, DoubtQueryService queries,
                        DoubtMapper mapper, UserLookup userLookup, HintDao hintDao, SubjectCatalog catalog,
                        RateLimiter limiter, Clock clock) {
        this.doubts = doubts;
        this.classification = classification;
        this.embeddings = embeddings;
        this.embeddingStore = embeddingStore;
        this.matching = matching;
        this.queries = queries;
        this.mapper = mapper;
        this.userLookup = userLookup;
        this.hintDao = hintDao;
        this.catalog = catalog;
        this.limiter = limiter;
        this.clock = clock;
    }

    /**
     * Creation pipeline: validate, classify (AI, outside any transaction), insert, embed (AI), match and notify.
     * Classification and embedding fail open, so a slow or broken model never blocks a student from posting.
     */
    public DoubtDetail create(CurrentUser me, CreateDoubtRequest req) {
        limiter.check(RateLimitKey.DOUBT_CREATE, String.valueOf(me.userId()));
        String title = ContentGuard.title(req.title());
        String body = ContentGuard.text(req.bodyMd());
        ContentGuard.rejectUnsafe(title, body);
        if (title.length() < 10 || body.length() < 20) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Title needs 10+ characters and the description needs 20+.");
        }
        Optional<SubjectInfo> chosen = Optional.empty();
        if (req.subjectId() != null) {
            chosen = Optional.of(catalog.byId(req.subjectId())
                    .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "That subject does not exist.")));
        }

        Optional<ClassifyResult> ai = classification.classify(title, body);
        if (ai.isPresent() && Boolean.FALSE.equals(ai.get().isAcademic())) {
            throw new ApiException(ErrorCode.NOT_ACADEMIC);
        }
        Optional<SubjectInfo> aiSubject = ai.flatMap(c -> catalog.byCode(c.subjectCode()));
        SubjectInfo subject = chosen.or(() -> aiSubject)
                .orElseThrow(() -> new ApiException(ErrorCode.SUBJECT_REQUIRED));
        TopicRef topic = aiSubject.isPresent() && aiSubject.get().id() == subject.id()
                ? catalog.topicByName(subject.id(), ai.get().topic()).orElseGet(() -> catalog.general(subject.id()))
                : catalog.general(subject.id());
        Integer difficulty = ai.map(ClassifyResult::difficulty).filter(v -> v != null && v >= 1 && v <= 3)
                .orElse(null);

        Doubt doubt = new Doubt();
        doubt.setCampusId(me.campusId());
        doubt.setAuthorId(me.userId());
        doubt.setSubjectId(subject.id());
        doubt.setTopicId(topic.id());
        doubt.setTitle(title);
        doubt.setBodyMd(body);
        doubt.setDifficulty(difficulty);
        doubt.setSource("SNAP".equals(req.source()) ? DoubtSource.SNAP : DoubtSource.TEXT);
        doubt.setCreatedAt(clock.instant());
        doubt = doubts.saveAndFlush(doubt);

        try {
            embeddingStore.saveDoubtEmbedding(doubt.getId(),
                    embeddings.embed(EmbeddingText.forDoubt(title, subject.name(), topic.name(), body)));
        } catch (AiUnavailableException e) {
            log.warn("Doubt {} saved without embedding ({}); backfill will retry", doubt.getId(), e.getCode());
        }
        try {
            matching.matchAndNotify(doubt);
        } catch (RuntimeException e) {
            log.error("Matching failed for doubt {}", doubt.getId(), e);
        }
        return queries.detail(me, doubt.getId(), false);
    }

    @Transactional
    public void delete(CurrentUser me, long id) {
        Doubt d = doubts.findByIdForUpdate(id).filter(x -> x.getCampusId().equals(me.campusId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!d.getAuthorId().equals(me.userId())) {
            throw new ApiException(ErrorCode.NOT_DOUBT_OWNER);
        }
        if (d.getAnswerCount() > 0) {
            throw new ApiException(ErrorCode.HAS_ANSWERS);
        }
        doubts.delete(d);
    }

    @Transactional
    public DoubtSummary resolveByHint(CurrentUser me, long id) {
        Doubt d = doubts.findByIdForUpdate(id).filter(x -> x.getCampusId().equals(me.campusId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!d.getAuthorId().equals(me.userId())) {
            throw new ApiException(ErrorCode.NOT_DOUBT_OWNER);
        }
        if (d.getStatus() != DoubtStatus.OPEN) {
            throw new ApiException(ErrorCode.DOUBT_SOLVED);
        }
        if (hintDao.levelUnlocked(me.userId(), id) < 1) {
            throw new ApiException(ErrorCode.NO_HINTS_USED);
        }
        d.setStatus(DoubtStatus.SOLVED);
        d.setResolution(DoubtResolution.AI_HINT);
        d.setSolvedAt(clock.instant());
        doubts.saveAndFlush(d);
        return mapper.summary(d, userLookup.briefs(java.util.List.of(d.getAuthorId())).get(d.getAuthorId()));
    }
}
