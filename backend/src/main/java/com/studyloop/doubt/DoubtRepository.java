package com.studyloop.doubt;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoubtRepository extends JpaRepository<Doubt, Long>, JpaSpecificationExecutor<Doubt> {

    Optional<Doubt> findByIdAndCampusId(Long id, Long campusId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doubt d where d.id = :id")
    Optional<Doubt> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("update Doubt d set d.viewCount = d.viewCount + 1 where d.id = :id")
    void incrementViews(@Param("id") Long id);

    @Modifying
    @Query("update Doubt d set d.answerCount = d.answerCount + 1, d.firstAnswerAt = coalesce(d.firstAnswerAt, :now) "
            + "where d.id = :id")
    void recordAnswer(@Param("id") Long id, @Param("now") Instant now);
}
