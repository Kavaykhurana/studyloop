package com.studyloop.answer;

import com.studyloop.answer.dto.AcceptResult;
import com.studyloop.answer.dto.AnswerDto;
import com.studyloop.answer.dto.PostAnswerRequest;
import com.studyloop.answer.dto.RatingRequest;
import com.studyloop.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Answers")
public class AnswerController {

    private final AnswerService answers;

    public AnswerController(AnswerService answers) {
        this.answers = answers;
    }

    @PostMapping("/doubts/{id}/answers")
    public ResponseEntity<AnswerDto> post(CurrentUser me, @PathVariable long id,
                                          @Valid @RequestBody PostAnswerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(answers.post(me, id, req.bodyMd()));
    }

    @PutMapping("/answers/{id}/accept")
    public AcceptResult accept(CurrentUser me, @PathVariable long id) {
        return answers.accept(me, id);
    }

    @PutMapping("/answers/{id}/rating")
    public AnswerDto rate(CurrentUser me, @PathVariable long id, @Valid @RequestBody RatingRequest req) {
        return answers.rate(me, id, req.stars());
    }
}
