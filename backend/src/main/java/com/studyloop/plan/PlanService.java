package com.studyloop.plan;

import com.studyloop.ai.AiFeature;
import com.studyloop.ai.AiRequest;
import com.studyloop.ai.AiResult;
import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.LlmClient;
import com.studyloop.ai.ModelTier;
import com.studyloop.ai.Prompts;
import com.studyloop.ai.Schemas;
import com.studyloop.ai.dto.PlanResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.text.ContentGuard;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.plan.dto.CreatePlanRequest;
import com.studyloop.plan.dto.PlanDto;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.subject.dto.TopicRef;
import com.studyloop.user.UserSubject;
import com.studyloop.user.UserSubjectRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PlanService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MAX_DAYS = 7;
    private static final int MAX_TASKS_PER_DAY = 8;

    private final StudyPlanRepository plans;
    private final PlanTaskRepository tasks;
    private final UserSubjectRepository userSubjects;
    private final SubjectCatalog catalog;
    private final LlmClient llm;
    private final Prompts prompts;
    private final Schemas schemas;
    private final RateLimiter limiter;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PlanService(StudyPlanRepository plans, PlanTaskRepository tasks, UserSubjectRepository userSubjects,
                       SubjectCatalog catalog, LlmClient llm, Prompts prompts, Schemas schemas, RateLimiter limiter,
                       JdbcTemplate jdbc, TransactionTemplate tx, Clock clock) {
        this.plans = plans;
        this.tasks = tasks;
        this.userSubjects = userSubjects;
        this.catalog = catalog;
        this.llm = llm;
        this.prompts = prompts;
        this.schemas = schemas;
        this.limiter = limiter;
        this.jdbc = jdbc;
        this.tx = tx;
        this.clock = clock;
    }

    public PlanDto create(CurrentUser me, CreatePlanRequest req) {
        limiter.check(RateLimitKey.PLAN, String.valueOf(me.userId()));
        String examName = ContentGuard.title(req.examName());
        ContentGuard.rejectUnsafe(examName);
        BigDecimal hours = req.hoursPerDay();
        if (hours.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Hours per day must go up in steps of 0.5.");
        }
        LocalDate today = today();
        if (req.examDate().isBefore(today.plusDays(1)) || req.examDate().isAfter(today.plusDays(60))) {
            throw new ApiException(ErrorCode.INVALID_EXAM_DATE);
        }
        Set<Long> subjectIds = new LinkedHashSet<>(req.subjectIds());
        List<SubjectInfo> chosen = new ArrayList<>();
        for (Long id : subjectIds) {
            chosen.add(catalog.byId(id).orElseThrow(
                    () -> new ApiException(ErrorCode.VALIDATION_FAILED, "That subject does not exist.")));
        }
        int numDays = (int) Math.min(MAX_DAYS, ChronoUnit.DAYS.between(today, req.examDate()));
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < numDays; i++) {
            dates.add(today.plusDays(i));
        }
        int budget = hours.multiply(BigDecimal.valueOf(60)).intValue();

        Map<String, List<String>> subjectTopics = new LinkedHashMap<>();
        chosen.forEach(s -> subjectTopics.put(s.code(), s.topics().stream().map(TopicRef::name).toList()));
        AiRequest request = new AiRequest(AiFeature.PLAN, ModelTier.CHAT, Prompts.PLAN_SYSTEM,
                prompts.planUser(examName, req.examDate().toString(), dates.stream().map(LocalDate::toString).toList(),
                        budget, subjectTopics, needsHelp(me.userId()), recentTopics(me.userId()),
                        openDoubtTitles(me.userId())), null, null, schemas.plan(), 2048, Duration.ofSeconds(20));
        AiResult<PlanResult> result;
        try {
            result = llm.generateJson(request, PlanResult.class);
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_UNAVAILABLE);
        }
        List<ValidDay> days = validate(result.value(), dates, budget);
        if (days.isEmpty()) {
            throw new ApiException(ErrorCode.AI_BAD_OUTPUT);
        }
        String summary = result.value().summary() == null || result.value().summary().isBlank()
                ? "A day-by-day plan built around your weak topics." : result.value().summary().trim();
        Long planId = tx.execute(status -> persist(me.userId(), examName, req.examDate(), hours,
                summary.length() <= 400 ? summary : summary.substring(0, 400), result.model(), days));
        return toDto(plans.findById(planId).orElseThrow());
    }

    record ValidTask(String title, int minutes, PlanTaskKind kind, long subjectId, Long topicId) {}

    record ValidDay(int dayIndex, LocalDate date, String focus, List<ValidTask> tasks) {}

    /** Keeps only what is safe to store: in-range dates, known subjects, tasks that fit the daily budget. */
    List<ValidDay> validate(PlanResult raw, List<LocalDate> dates, int budgetMinutes) {
        TreeMap<LocalDate, ValidDay> byDate = new TreeMap<>();
        if (raw == null || raw.days() == null) {
            return List.of();
        }
        for (PlanResult.Day day : raw.days()) {
            LocalDate date;
            try {
                date = LocalDate.parse(day.date() == null ? "" : day.date().trim());
            } catch (DateTimeParseException e) {
                continue;
            }
            int index = dates.indexOf(date);
            if (index < 0 || byDate.containsKey(date) || day.tasks() == null) {
                continue;
            }
            List<ValidTask> kept = new ArrayList<>();
            int total = 0;
            for (PlanResult.Task t : day.tasks()) {
                if (kept.size() >= MAX_TASKS_PER_DAY || t == null || t.title() == null || t.title().isBlank()) {
                    continue;
                }
                Optional<SubjectInfo> subject = catalog.byCode(t.subjectCode());
                if (subject.isEmpty()) {
                    continue;
                }
                int minutes = Math.max(10, Math.min(240, t.minutes() == null ? 30 : t.minutes()));
                if (total + minutes > budgetMinutes) {
                    break;
                }
                total += minutes;
                Long topicId = catalog.topicByName(subject.get().id(), t.topic()).map(TopicRef::id).orElse(null);
                String title = t.title().trim();
                kept.add(new ValidTask(title.length() <= 140 ? title : title.substring(0, 140), minutes, kind(t.kind()),
                        subject.get().id(), topicId));
            }
            if (!kept.isEmpty()) {
                String focus = day.focus() == null || day.focus().isBlank() ? "Study day" : day.focus().trim();
                byDate.put(date, new ValidDay(index, date, focus.length() <= 80 ? focus : focus.substring(0, 80),
                        kept));
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private Long persist(long userId, String examName, LocalDate examDate, BigDecimal hours, String summary,
                         String model, List<ValidDay> days) {
        plans.deactivateAll(userId);
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setExamName(examName);
        plan.setExamDate(examDate);
        plan.setHoursPerDay(hours);
        plan.setSummary(summary);
        plan.setModel(model);
        plan.setActive(true);
        plan.setCreatedAt(clock.instant());
        plan = plans.saveAndFlush(plan);
        for (ValidDay day : days) {
            int position = 0;
            for (ValidTask t : day.tasks()) {
                PlanTask task = new PlanTask();
                task.setPlanId(plan.getId());
                task.setDayIndex(day.dayIndex());
                task.setPlanDate(day.date());
                task.setDayFocus(day.focus());
                task.setPosition(position++);
                task.setTitle(t.title());
                task.setMinutes(t.minutes());
                task.setKind(t.kind());
                task.setSubjectId(t.subjectId());
                task.setTopicId(t.topicId());
                tasks.save(task);
            }
        }
        return plan.getId();
    }

    @Transactional(readOnly = true)
    public Optional<PlanDto> active(long userId) {
        return plans.findByUserIdAndActiveTrue(userId).map(this::toDto);
    }

    @Transactional
    public PlanDto.Task toggle(long userId, long taskId, boolean done) {
        PlanTask task = tasks.findById(taskId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        StudyPlan plan = plans.findById(task.getPlanId()).filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        task.setDoneAt(done ? clock.instant() : null);
        tasks.saveAndFlush(task);
        return toTaskDto(task);
    }

    private PlanDto toDto(StudyPlan plan) {
        List<PlanTask> rows = tasks.findByPlanIdOrderByDayIndexAscPositionAsc(plan.getId());
        Map<Integer, List<PlanTask>> byDay = new TreeMap<>();
        rows.forEach(t -> byDay.computeIfAbsent(t.getDayIndex(), k -> new ArrayList<>()).add(t));
        List<PlanDto.Day> days = new ArrayList<>();
        byDay.forEach((index, list) -> days.add(new PlanDto.Day(index, list.get(0).getPlanDate(),
                list.get(0).getDayFocus(), list.stream().mapToInt(PlanTask::getMinutes).sum(),
                list.stream().map(this::toTaskDto).toList())));
        long done = rows.stream().filter(t -> t.getDoneAt() != null).count();
        return new PlanDto(plan.getId(), plan.getExamName(), plan.getExamDate(), plan.getHoursPerDay(),
                plan.getSummary(), plan.getCreatedAt(), new PlanDto.Progress(done, rows.size()), days);
    }

    private PlanDto.Task toTaskDto(PlanTask t) {
        return new PlanDto.Task(t.getId(), t.getTitle(), t.getMinutes(), t.getKind().name(),
                catalog.ref(t.getSubjectId()), catalog.topic(t.getTopicId()).orElse(null), t.getDoneAt() != null);
    }

    private static PlanTaskKind kind(String raw) {
        try {
            return PlanTaskKind.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlanTaskKind.LEARN;
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(IST));
    }

    private Map<String, Integer> needsHelp(long userId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        userSubjects.findByUserId(userId).stream().filter(us -> us.getKind() == UserSubject.Kind.NEEDS_HELP)
                .forEach(us -> catalog.byId(us.getSubjectId()).ifPresent(s -> result.put(s.code(), us.getLevel())));
        return result;
    }

    private Map<String, Integer> recentTopics(long userId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Timestamp since = Timestamp.from(clock.instant().minus(Duration.ofDays(30)));
        jdbc.query("""
                SELECT subject_id, topic_id, COUNT(*) AS n FROM doubts
                WHERE author_id = ? AND created_at > ? AND topic_id IS NOT NULL
                GROUP BY subject_id, topic_id ORDER BY n DESC, topic_id""", rs -> {
            String code = catalog.byId(rs.getLong("subject_id")).map(SubjectInfo::code).orElse(null);
            String topic = catalog.topic(rs.getLong("topic_id")).map(TopicRef::name).orElse(null);
            if (code != null && topic != null) {
                result.put(code + "/" + topic, rs.getInt("n"));
            }
        }, userId, since);
        return result;
    }

    private List<String> openDoubtTitles(long userId) {
        return jdbc.queryForList("""
                SELECT title FROM doubts WHERE author_id = ? AND status = 'OPEN'
                ORDER BY created_at DESC LIMIT 5""", String.class, userId);
    }
}
