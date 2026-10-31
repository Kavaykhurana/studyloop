package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.studyloop.ai.AiRequest;
import com.studyloop.ai.AiResult;
import com.studyloop.ai.LlmClient;
import com.studyloop.ai.dto.PlanResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.plan.PlanService;
import com.studyloop.plan.dto.CreatePlanRequest;
import com.studyloop.plan.dto.PlanDto;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PlanServiceTest extends IntegrationTestBase {

    @MockitoBean
    LlmClient llm;
    @Autowired
    PlanService plans;

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata")));
    }

    private PlanResult.Task task(String title, int minutes, String subject) {
        return new PlanResult.Task(title, minutes, "PRACTICE", subject, "Normalization");
    }

    private void llmReturns(PlanResult result) {
        when(llm.generateJson(any(AiRequest.class), eq(PlanResult.class)))
                .thenReturn(new AiResult<>(result, "test-model", 10));
    }

    private CreatePlanRequest request(int daysAhead) {
        return new CreatePlanRequest("DBMS Mid-Sem", today().plusDays(daysAhead), new BigDecimal("2.0"),
                List.of(fx.subjectId("DBMS")));
    }

    @Test
    void tasksBeyondTheDailyBudgetAreTrimmedAndUnknownSubjectsDropped() {
        User user = fx.tiet("Asha Learner");
        LocalDate d0 = today();
        llmReturns(new PlanResult("Practise daily.", List.of(
                new PlanResult.Day(d0.toString(), "Normal forms", List.of(
                        task("Solve 5 BCNF questions", 60, "DBMS"),
                        task("Write notes on a made-up subject", 30, "ZZZ"),
                        task("Revise closure rules", 60, "DBMS"),
                        task("Solve past paper", 60, "DBMS"))),
                new PlanResult.Day(d0.plusDays(30).toString(), "Out of range", List.of(task("Ignored", 30, "DBMS"))))));

        PlanDto plan = plans.create(fx.as(user), request(3));

        assertThat(plan.days()).hasSize(1);
        PlanDto.Day day = plan.days().get(0);
        assertThat(day.totalMinutes()).isLessThanOrEqualTo(120);
        assertThat(day.tasks()).extracting(PlanDto.Task::title)
                .containsExactly("Solve 5 BCNF questions", "Revise closure rules");
        assertThat(plan.progress().total()).isEqualTo(2);
    }

    @Test
    void creatingANewPlanDeactivatesThePreviousOne() {
        User user = fx.tiet("Asha Learner");
        llmReturns(new PlanResult("Plan.", List.of(new PlanResult.Day(today().toString(), "Focus",
                List.of(task("Solve 5 questions", 60, "DBMS"))))));

        PlanDto first = plans.create(fx.as(user), request(3));
        PlanDto second = plans.create(fx.as(user), request(4));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM study_plans WHERE user_id = ? AND is_active",
                Integer.class, user.getId())).isEqualTo(1);
        assertThat(plans.active(user.getId()).orElseThrow().id()).isEqualTo(second.id());
    }

    @Test
    void examDatesOutsideTheAllowedWindowAreRejected() {
        User user = fx.tiet("Asha Learner");
        for (int daysAhead : new int[] {0, -2, 61}) {
            assertThatThrownBy(() -> plans.create(fx.as(user), request(daysAhead)))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_EXAM_DATE));
        }
    }

    @Test
    void anOutputWithNoUsableDaysIsABadGatewayError() {
        User user = fx.tiet("Asha Learner");
        llmReturns(new PlanResult("Nothing useful.", List.of(
                new PlanResult.Day("not-a-date", "Focus", List.of(task("Solve 5 questions", 60, "DBMS"))))));

        assertThatThrownBy(() -> plans.create(fx.as(user), request(3))).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.AI_BAD_OUTPUT));
    }

    @Test
    void onlyTheOwnerCanTickATask() {
        User owner = fx.tiet("Asha Learner");
        User other = fx.tiet("Nina Other");
        llmReturns(new PlanResult("Plan.", List.of(new PlanResult.Day(today().toString(), "Focus",
                List.of(task("Solve 5 questions", 60, "DBMS"))))));
        PlanDto plan = plans.create(fx.as(owner), request(3));
        long taskId = plan.days().get(0).tasks().get(0).id();

        assertThat(plans.toggle(owner.getId(), taskId, true).done()).isTrue();
        assertThat(plans.active(owner.getId()).orElseThrow().progress().done()).isEqualTo(1);
        assertThatThrownBy(() -> plans.toggle(other.getId(), taskId, true)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
