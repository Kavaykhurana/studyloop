package com.studyloop.answer;

import com.studyloop.answer.dto.AnswerDto;
import com.studyloop.user.UserLookup;
import com.studyloop.user.dto.UserBrief;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AnswerMapper {

    private final UserLookup userLookup;

    public AnswerMapper(UserLookup userLookup) {
        this.userLookup = userLookup;
    }

    /** Accepted answer first, then oldest first. */
    public List<AnswerDto> toDtos(List<Answer> answers, long viewerId) {
        Map<Long, UserBrief> authors = userLookup.briefs(answers.stream().map(Answer::getAuthorId).toList());
        return answers.stream()
                .sorted(Comparator.comparing((Answer a) -> !a.isAccepted())
                        .thenComparing(Answer::getCreatedAt).thenComparing(Answer::getId))
                .map(a -> toDto(a, authors.get(a.getAuthorId()), viewerId))
                .toList();
    }

    public AnswerDto toDto(Answer a, UserBrief author, long viewerId) {
        return new AnswerDto(a.getId(), a.getDoubtId(), a.getBodyMd(), author, a.isAccepted(), a.getHelpfulRating(),
                a.getCreatedAt(), a.getAcceptedAt(), a.getAuthorId().equals(viewerId));
    }
}
