package com.studyloop.plan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanTaskRepository extends JpaRepository<PlanTask, Long> {

    List<PlanTask> findByPlanIdOrderByDayIndexAscPositionAsc(Long planId);
}
