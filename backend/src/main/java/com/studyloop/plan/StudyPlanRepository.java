package com.studyloop.plan;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    Optional<StudyPlan> findByUserIdAndActiveTrue(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StudyPlan p set p.active = false where p.userId = :userId and p.active = true")
    int deactivateAll(@Param("userId") Long userId);
}
