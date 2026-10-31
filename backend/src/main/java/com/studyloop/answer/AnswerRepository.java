package com.studyloop.answer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByDoubtIdOrderByCreatedAtAscIdAsc(Long doubtId);
}
