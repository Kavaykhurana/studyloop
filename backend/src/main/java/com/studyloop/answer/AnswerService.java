package com.studyloop.answer;

import com.studyloop.answer.dto.AcceptResult;
import com.studyloop.answer.dto.AnswerDto;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.text.ContentGuard;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.doubt.Doubt;
import com.studyloop.doubt.DoubtRepository;
import com.studyloop.doubt.DoubtResolution;
import com.studyloop.doubt.DoubtStatus;
import com.studyloop.notification.NotificationService;
import com.studyloop.notification.NotificationType;
import com.studyloop.reputation.ReputationReason;
import com.studyloop.reputation.ReputationService;
import com.studyloop.user.User;
import com.studyloop.user.UserLookup;
import com.studyloop.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnswerService {

    private static final Duration FAST_WINDOW = Duration.ofMinutes(30);

    private final AnswerRepository answers;
    private final DoubtRepository doubts;
    private final UserRepository users;
    private final ReputationService reputation;
    private final NotificationService notifications;
    private final AnswerMapper mapper;
    private final UserLookup userLookup;
    private final RateLimiter limiter;
    private final Clock clock;

    public AnswerService(AnswerRepository answers, DoubtRepository doubts, UserRepository users,
                         ReputationService reputation, NotificationService notifications, AnswerMapper mapper,
                         UserLookup userLookup, RateLimiter limiter, Clock clock) {
        this.answers = answers;
        this.doubts = doubts;
        this.users = users;
        this.reputation = reputation;
        this.notifications = notifications;
        this.mapper = mapper;
        this.userLookup = userLookup;
        this.limiter = limiter;
        this.clock = clock;
    }

    @Transactional
    public AnswerDto post(CurrentUser me, long doubtId, String bodyMd) {
        limiter.check(RateLimitKey.ANSWER, String.valueOf(me.userId()));
        String body = ContentGuard.text(bodyMd);
        ContentGuard.rejectUnsafe(body);
        if (body.length() < 20 || body.length() > 10000) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Answer must be 20 to 10000 characters.");
        }
        Doubt doubt = doubts.findByIdForUpdate(doubtId)
                .filter(d -> d.getCampusId().equals(me.campusId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (doubt.getAuthorId().equals(me.userId())) {
            throw new ApiException(ErrorCode.CANNOT_ANSWER_OWN);
        }
        if (doubt.getStatus() != DoubtStatus.OPEN) {
            throw new ApiException(ErrorCode.DOUBT_SOLVED);
        }
        Instant now = clock.instant();
        Answer answer = new Answer();
        answer.setDoubtId(doubtId);
        answer.setAuthorId(me.userId());
        answer.setBodyMd(body);
        answer.setCreatedAt(now);
        answer = answers.saveAndFlush(answer);
        doubts.recordAnswer(doubtId, now);
        User helper = users.findById(me.userId()).orElseThrow();
        helper.setLastActiveAt(now);
        String title = doubt.getTitle().length() <= 60 ? doubt.getTitle() : doubt.getTitle().substring(0, 60);
        notifications.create(doubt.getAuthorId(), NotificationType.NEW_ANSWER, doubtId, helper.getId(),
                helper.firstName() + " answered your doubt \"" + title + "\"");
        return mapper.toDto(answer, UserLookup.brief(helper), me.userId());
    }

    /** Author accepts an answer. The doubt row is locked so concurrent accepts serialise; one wins. */
    @Transactional
    public AcceptResult accept(CurrentUser me, long answerId) {
        Answer answer = answers.findById(answerId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Doubt doubt = doubts.findByIdForUpdate(answer.getDoubtId())
                .filter(d -> d.getCampusId().equals(me.campusId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!doubt.getAuthorId().equals(me.userId())) {
            throw new ApiException(ErrorCode.NOT_DOUBT_OWNER);
        }
        if (doubt.getStatus() != DoubtStatus.OPEN) {
            throw new ApiException(ErrorCode.DOUBT_SOLVED);
        }
        Instant now = clock.instant();
        answer.setAccepted(true);
        answer.setAcceptedAt(now);
        doubt.setStatus(DoubtStatus.SOLVED);
        doubt.setResolution(DoubtResolution.PEER_ANSWER);
        doubt.setSolvedAt(now);
        try {
            answers.saveAndFlush(answer);
            doubts.saveAndFlush(doubt);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.ALREADY_ACCEPTED);
        }
        int helperPoints = reputation.award(answer.getAuthorId(), doubt.getSubjectId(),
                ReputationReason.ANSWER_ACCEPTED, answerId);
        if (Duration.between(doubt.getCreatedAt(), answer.getCreatedAt()).compareTo(FAST_WINDOW) <= 0) {
            helperPoints += reputation.award(answer.getAuthorId(), doubt.getSubjectId(),
                    ReputationReason.FAST_ANSWER, answerId);
        }
        int authorPoints = reputation.award(doubt.getAuthorId(), doubt.getSubjectId(),
                ReputationReason.LOOP_CLOSED, answerId);
        User author = users.findById(doubt.getAuthorId()).orElseThrow();
        notifications.create(answer.getAuthorId(), NotificationType.ANSWER_ACCEPTED, doubt.getId(), author.getId(),
                author.firstName() + " accepted your answer. +" + helperPoints + " reputation");
        return new AcceptResult(doubt.getId(), answerId, new AcceptResult.Awarded(helperPoints, authorPoints));
    }

    @Transactional
    public AnswerDto rate(CurrentUser me, long answerId, int stars) {
        Answer answer = answers.findById(answerId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Doubt doubt = doubts.findByIdAndCampusId(answer.getDoubtId(), me.campusId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!doubt.getAuthorId().equals(me.userId())) {
            throw new ApiException(ErrorCode.NOT_DOUBT_OWNER);
        }
        answer.setHelpfulRating(stars);
        answers.saveAndFlush(answer);
        if (stars >= 4) {
            reputation.award(answer.getAuthorId(), doubt.getSubjectId(), ReputationReason.HELPFUL_RATING, answerId);
        }
        return mapper.toDto(answer, userLookup.briefs(java.util.List.of(answer.getAuthorId()))
                .get(answer.getAuthorId()), me.userId());
    }
}
