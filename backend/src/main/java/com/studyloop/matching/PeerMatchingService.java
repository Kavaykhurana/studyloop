package com.studyloop.matching;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.config.AppProperties;
import com.studyloop.doubt.Doubt;
import com.studyloop.doubt.DoubtRepository;
import com.studyloop.doubt.DoubtStatus;
import com.studyloop.embedding.EmbeddingStore;
import com.studyloop.matching.dto.MatchDto;
import com.studyloop.notification.NotificationService;
import com.studyloop.notification.NotificationType;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.user.User;
import com.studyloop.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hybrid expertise routing. score = round(100 * (0.40 semantic + 0.20 declared + 0.20 reputation + 0.20 recency)
 * - 15 * load), clamped to 0..100.
 */
@Service
public class PeerMatchingService {

    static final double W_SEMANTIC = 0.40;
    static final double W_DECLARED = 0.20;
    static final double W_REPUTATION = 0.20;
    static final double W_RECENCY = 0.20;
    static final double LOAD_PENALTY = 15;
    static final int KEEP = 5;
    static final int NOTIFY_TOP = 3;
    static final int NOTIFY_MIN_SCORE = 25;

    private record Scored(long helperId, int score, double semantic, double declared, double reputation,
                          double recency, double load, List<String> reasons) {}

    private final MatchCandidateDao dao;
    private final MatchStore store;
    private final EmbeddingStore embeddings;
    private final DoubtRepository doubts;
    private final UserRepository users;
    private final NotificationService notifications;
    private final SubjectCatalog catalog;
    private final AppProperties props;
    private final Clock clock;

    public PeerMatchingService(MatchCandidateDao dao, MatchStore store, EmbeddingStore embeddings,
                               DoubtRepository doubts, UserRepository users, NotificationService notifications,
                               SubjectCatalog catalog, AppProperties props, Clock clock) {
        this.dao = dao;
        this.store = store;
        this.embeddings = embeddings;
        this.doubts = doubts;
        this.users = users;
        this.notifications = notifications;
        this.catalog = catalog;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public List<MatchDto> refresh(CurrentUser me, long doubtId) {
        Doubt doubt = doubts.findByIdAndCampusId(doubtId, me.campusId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!doubt.getAuthorId().equals(me.userId())) {
            throw new ApiException(ErrorCode.NOT_DOUBT_OWNER);
        }
        if (doubt.getStatus() != DoubtStatus.OPEN) {
            throw new ApiException(ErrorCode.DOUBT_SOLVED);
        }
        return matchAndNotify(doubt);
    }

    /** Ranks helpers for the doubt, persists the top five, and notifies the top three that qualify. */
    @Transactional
    public List<MatchDto> matchAndNotify(Doubt doubt) {
        return match(doubt, true);
    }

    /**
     * Same ranking and persistence, but qualifying helpers are only flagged as notified. Used when seeding, where
     * the notification rows are written explicitly.
     */
    @Transactional
    public List<MatchDto> matchWithoutNotifying(Doubt doubt) {
        return match(doubt, false);
    }

    private List<MatchDto> match(Doubt doubt, boolean sendNotifications) {
        List<Scored> ranked = rank(doubt);
        Map<Long, Boolean> alreadyNotified = store.notifiedByHelper(doubt.getId());
        User author = users.findById(doubt.getAuthorId()).orElseThrow();
        SubjectInfo subject = catalog.byId(doubt.getSubjectId()).orElseThrow();
        String topicName = catalog.topic(doubt.getTopicId()).map(t -> t.name()).orElse(SubjectCatalog.GENERAL);

        List<Long> kept = new ArrayList<>();
        int rank = 1;
        for (Scored s : ranked) {
            Map<String, Double> breakdown = new LinkedHashMap<>();
            breakdown.put("semantic", round2(s.semantic()));
            breakdown.put("declared", round2(s.declared()));
            breakdown.put("reputation", round2(s.reputation()));
            breakdown.put("recency", round2(s.recency()));
            breakdown.put("load", round2(s.load()));
            store.upsert(doubt.getId(), s.helperId(), rank, s.score(), breakdown, s.reasons());
            kept.add(s.helperId());
            boolean notify = rank <= NOTIFY_TOP && s.score() >= NOTIFY_MIN_SCORE
                    && !Boolean.TRUE.equals(alreadyNotified.get(s.helperId()));
            if (notify) {
                if (sendNotifications) {
                    notifications.create(s.helperId(), NotificationType.MATCHED, doubt.getId(), author.getId(),
                            author.firstName() + " needs help with " + topicName + " (" + subject.code() + ")");
                }
                store.markNotified(doubt.getId(), s.helperId());
            }
            rank++;
        }
        store.deleteExcept(doubt.getId(), kept);
        return store.find(doubt.getId());
    }

    private List<Scored> rank(Doubt doubt) {
        long campus = doubt.getCampusId();
        long subject = doubt.getSubjectId();
        long author = doubt.getAuthorId();
        Instant now = clock.instant();
        AppProperties.Similarity sim = props.similarity();

        Map<Long, MatchCandidateDao.Semantic> semantic = Map.of();
        Optional<String> vector = embeddings.getDoubtEmbedding(doubt.getId());
        if (vector.isPresent()) {
            semantic = dao.semantic(vector.get(), campus, subject, author, now.minus(Duration.ofDays(180)),
                    sim.groundingThreshold());
        }
        Map<Long, Integer> strong = dao.strongLevels(campus, subject, author);
        Set<Long> candidates = new HashSet<>(strong.keySet());
        candidates.addAll(dao.acceptedAuthors(campus, subject, author));
        candidates.remove(author);
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<Long, Instant> lastActive = dao.lastActive(candidates);
        Map<Long, Integer> reputation = dao.subjectReputation(subject, candidates);
        int maxRep = dao.maxSubjectReputation(subject, campus);
        Map<Long, Integer> openMatches = dao.openMatches(candidates, now.minus(Duration.ofHours(48)), doubt.getId());
        String subjectCode = catalog.byId(subject).orElseThrow().code();
        String topicName = catalog.topic(doubt.getTopicId()).map(t -> t.name()).orElse(SubjectCatalog.GENERAL);

        List<Scored> scored = new ArrayList<>();
        for (Long id : candidates) {
            MatchCandidateDao.Semantic stat = semantic.get(id);
            double semanticScore = stat == null ? 0
                    : clamp((stat.bestSim() - sim.semanticFloor()) / (sim.semanticCeil() - sim.semanticFloor()));
            Integer level = strong.get(id);
            double declared = level == null ? 0 : level / 5.0;
            int subjRep = reputation.getOrDefault(id, 0);
            double repScore = maxRep == 0 ? 0 : Math.log(1 + subjRep) / Math.log(1 + maxRep);
            double recency = recency(lastActive.get(id), now);
            int open = openMatches.getOrDefault(id, 0);
            double load = Math.min(open, 3) / 3.0;
            double raw = 100 * (W_SEMANTIC * semanticScore + W_DECLARED * declared + W_REPUTATION * repScore
                    + W_RECENCY * recency) - LOAD_PENALTY * load;
            int score = (int) Math.max(0, Math.min(100, Math.round(raw)));
            List<String> reasons = reasons(semanticScore, stat, topicName, level, subjectCode, repScore, subjRep,
                    recency, open, load);
            scored.add(new Scored(id, score, semanticScore, declared, repScore, recency, load, reasons));
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(Comparator.comparingDouble(Scored::semantic).reversed())
                .thenComparing(Comparator.comparingDouble(Scored::reputation).reversed())
                .thenComparingLong(Scored::helperId));
        return scored.subList(0, Math.min(KEEP, scored.size()));
    }

    static List<String> reasons(double semantic, MatchCandidateDao.Semantic stat, String topicName, Integer level,
                                String subjectCode, double repScore, int subjRep, double recency, int open,
                                double load) {
        List<String> reasons = new ArrayList<>();
        if (semantic >= 0.5) {
            int n = Math.max(1, stat == null ? 0 : stat.similarSolved());
            reasons.add("Solved " + n + " similar " + topicName + (n == 1 ? " doubt" : " doubts"));
        }
        if (level != null && level > 0) {
            reasons.add("Rates " + subjectCode + " " + level + "/5");
        }
        if (repScore >= 0.5) {
            reasons.add(subjRep + " reputation in " + subjectCode);
        }
        if (recency == 1.0) {
            reasons.add("Active in the last 24 hours");
        } else if (recency == 0.6) {
            reasons.add("Active in the last 3 days");
        }
        if (load > 0) {
            reasons.add("Already helping with " + open + " open " + (open == 1 ? "doubt" : "doubts"));
        }
        return reasons.size() <= 3 ? reasons : new ArrayList<>(reasons.subList(0, 3));
    }

    static double recency(Instant lastActive, Instant now) {
        if (lastActive == null) {
            return 0;
        }
        Duration idle = Duration.between(lastActive, now);
        if (idle.compareTo(Duration.ofHours(24)) <= 0) {
            return 1.0;
        }
        if (idle.compareTo(Duration.ofHours(72)) <= 0) {
            return 0.6;
        }
        if (idle.compareTo(Duration.ofDays(7)) <= 0) {
            return 0.3;
        }
        return 0;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
