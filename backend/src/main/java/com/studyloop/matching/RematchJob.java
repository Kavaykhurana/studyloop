package com.studyloop.matching;

import com.studyloop.doubt.DoubtRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Gives unanswered doubts one more routing pass once the first matches have gone stale (30 to 180 minutes). */
@Component
public class RematchJob {

    private static final Logger log = LoggerFactory.getLogger(RematchJob.class);

    private final MatchCandidateDao dao;
    private final PeerMatchingService matching;
    private final DoubtRepository doubts;
    private final Clock clock;
    private final Set<Long> alreadyRematched = ConcurrentHashMap.newKeySet();

    public RematchJob(MatchCandidateDao dao, PeerMatchingService matching, DoubtRepository doubts, Clock clock) {
        this.dao = dao;
        this.matching = matching;
        this.doubts = doubts;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void run() {
        runOnce();
    }

    public int runOnce() {
        Instant now = clock.instant();
        int done = 0;
        for (Long id : dao.rematchCandidates(now.minus(Duration.ofMinutes(180)), now.minus(Duration.ofMinutes(30)),
                now.minus(Duration.ofMinutes(30)))) {
            if (!alreadyRematched.add(id)) {
                continue;
            }
            try {
                doubts.findById(id).ifPresent(matching::matchAndNotify);
                done++;
            } catch (RuntimeException e) {
                log.warn("Re-match failed for doubt {}: {}", id, e.getMessage());
            }
        }
        return done;
    }
}
