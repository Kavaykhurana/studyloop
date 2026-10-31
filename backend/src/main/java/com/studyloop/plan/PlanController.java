package com.studyloop.plan;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.plan.dto.CreatePlanRequest;
import com.studyloop.plan.dto.PlanDto;
import com.studyloop.plan.dto.TaskToggleRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "Study planner")
public class PlanController {

    private final PlanService plans;

    public PlanController(PlanService plans) {
        this.plans = plans;
    }

    @PostMapping
    public ResponseEntity<PlanDto> create(CurrentUser me, @Valid @RequestBody CreatePlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plans.create(me, req));
    }

    @GetMapping("/active")
    public ResponseEntity<PlanDto> active(CurrentUser me) {
        return plans.active(me.userId()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/tasks/{taskId}")
    public PlanDto.Task toggle(CurrentUser me, @PathVariable long taskId,
                               @Valid @RequestBody TaskToggleRequest req) {
        return plans.toggle(me.userId(), taskId, req.done());
    }
}
