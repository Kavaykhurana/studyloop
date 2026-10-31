package com.studyloop.doubt;

import com.studyloop.common.text.PlainText;
import com.studyloop.doubt.dto.DoubtSummary;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.dto.TopicRef;
import com.studyloop.user.UserLookup;
import com.studyloop.user.dto.UserBrief;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DoubtMapper {

    private static final int EXCERPT_CHARS = 160;

    private final DoubtRepository doubts;
    private final UserLookup userLookup;
    private final SubjectCatalog catalog;

    public DoubtMapper(DoubtRepository doubts, UserLookup userLookup, SubjectCatalog catalog) {
        this.doubts = doubts;
        this.userLookup = userLookup;
        this.catalog = catalog;
    }

    /** Maps a page of doubts with one batched author lookup. */
    @Transactional(readOnly = true)
    public List<DoubtSummary> summaries(List<Doubt> list) {
        Map<Long, UserBrief> authors = userLookup.briefs(list.stream().map(Doubt::getAuthorId).toList());
        return list.stream().map(d -> summary(d, authors.get(d.getAuthorId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<DoubtSummary> summariesInOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Doubt> byId = doubts.findAllById(ids).stream().collect(Collectors.toMap(Doubt::getId, d -> d));
        return summaries(ids.stream().map(byId::get).filter(Objects::nonNull).toList());
    }

    public DoubtSummary summary(Doubt d, UserBrief author) {
        TopicRef topic = catalog.topic(d.getTopicId()).orElse(null);
        return new DoubtSummary(d.getId(), d.getTitle(), PlainText.excerpt(d.getBodyMd(), EXCERPT_CHARS),
                catalog.ref(d.getSubjectId()), topic, d.getDifficulty(), d.getStatus().name(),
                d.getResolution() == null ? null : d.getResolution().name(), d.getSource().name(), author,
                d.getAnswerCount(), d.getCreatedAt(), d.getSolvedAt());
    }
}
