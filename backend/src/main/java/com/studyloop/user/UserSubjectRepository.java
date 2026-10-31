package com.studyloop.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSubjectRepository extends JpaRepository<UserSubject, Long> {

    List<UserSubject> findByUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserSubject us where us.userId = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
