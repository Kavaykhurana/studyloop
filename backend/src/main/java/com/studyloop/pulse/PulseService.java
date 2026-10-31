package com.studyloop.pulse;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.pulse.dto.PublicStats;
import com.studyloop.pulse.dto.PulseDto;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PulseService {

    static final long PUBLIC_CAMPUS_ID = 1L;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Set<Integer> RANGES = Set.of(7, 14, 30);
    private static final Duration PUBLIC_CACHE = Duration.ofSeconds(60);
    private static final Instant ALL_TIME = Instant.EPOCH;

    private final PulseDao dao;
    private final SubjectCatalog catalog;
    private final Clock clock;
    private volatile PublicStats cachedStats;
    private volatile Instant cachedAt = Instant.EPOCH;

    public PulseService(PulseDao dao, SubjectCatalog catalog, Clock clock) {
        this.dao = dao;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PulseDto pulse(long campusId, int days) {
        if (!RANGES.contains(days)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "days must be 7, 14 or 30.");
        }
        Instant now = clock.instant();
        Instant since = now.minus(Duration.ofDays(days));
        LocalDate today = LocalDate.now(clock.withZone(IST));

        PulseDao.DoubtTotals t = dao.doubtTotals(campusId, since);
        long deflections = dao.deflections(campusId, since);
        PulseDto.Totals totals = new PulseDto.Totals(t.posted(), t.solved(),
                t.posted() == 0 ? 0 : Math.min(1.0, (double) t.solved() / t.posted()), t.solvedByPeers(),
                t.solvedByHints(), deflections, selfServe(t.solvedByHints(), deflections, t.solved()),
                roundMinutes(t.median()), dao.activeHelpers(campusId, since));
        return new PulseDto(dao.campusShortName(campusId), days, dao.hasDemoUsers(campusId), totals,
                dao.daily(campusId, today.minusDays(days - 1L), today), confusionMap(campusId, since),
                dao.topHelpers(campusId, since), dao.aiHealth(since));
    }

    /** Landing-page numbers for the public demo campus, cached for 60 seconds. */
    @Transactional(readOnly = true)
    public PublicStats publicStats() {
        Instant now = clock.instant();
        PublicStats cached = cachedStats;
        if (cached != null && Duration.between(cachedAt, now).compareTo(PUBLIC_CACHE) < 0) {
            return cached;
        }
        PulseDao.DoubtTotals t = dao.doubtTotals(PUBLIC_CAMPUS_ID, ALL_TIME);
        long deflections = dao.deflections(PUBLIC_CAMPUS_ID, ALL_TIME);
        PublicStats fresh = new PublicStats(t.solved(), selfServe(t.solvedByHints(), deflections, t.solved()),
                roundMinutes(t.median()), dao.hasDemoUsers(PUBLIC_CAMPUS_ID));
        cachedStats = fresh;
        cachedAt = now;
        return fresh;
    }

    private List<PulseDto.SubjectConfusion> confusionMap(long campusId, Instant since) {
        Map<Long, List<PulseDto.TopicCount>> bySubject = new LinkedHashMap<>();
        for (PulseDao.TopicRow row : dao.topicCounts(campusId, since)) {
            List<PulseDto.TopicCount> list = bySubject.computeIfAbsent(row.subjectId(), k -> new ArrayList<>());
            if (list.size() < 5) {
                catalog.topic(row.topicId()).ifPresent(topic -> list.add(
                        new PulseDto.TopicCount(topic.id(), topic.name(), row.count(), row.openCount())));
            }
        }
        List<PulseDto.SubjectConfusion> result = new ArrayList<>();
        for (SubjectInfo subject : catalog.all()) {
            List<PulseDto.TopicCount> topics = bySubject.get(subject.id());
            if (topics != null && !topics.isEmpty()) {
                result.add(new PulseDto.SubjectConfusion(subject.ref(), topics));
            }
        }
        return result;
    }

    static double selfServe(long solvedByHints, long deflections, long solved) {
        long denominator = solved + deflections;
        return denominator == 0 ? 0 : (double) (solvedByHints + deflections) / denominator;
    }

    private static Double roundMinutes(Double minutes) {
        return minutes == null ? null : Math.round(minutes * 10.0) / 10.0;
    }
}
