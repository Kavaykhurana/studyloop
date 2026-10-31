package com.studyloop.doubt;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.common.web.PageResponse;
import com.studyloop.doubt.dto.CreateDoubtRequest;
import com.studyloop.doubt.dto.DeflectionRequest;
import com.studyloop.doubt.dto.DoubtDetail;
import com.studyloop.doubt.dto.DoubtSummary;
import com.studyloop.doubt.dto.SimilarRequest;
import com.studyloop.doubt.dto.SimilarResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doubts")
@Tag(name = "Doubts")
public class DoubtController {

    private final DoubtService doubts;
    private final DoubtQueryService queries;
    private final DuplicateRadarService radar;

    public DoubtController(DoubtService doubts, DoubtQueryService queries, DuplicateRadarService radar) {
        this.doubts = doubts;
        this.queries = queries;
        this.radar = radar;
    }

    @GetMapping
    public PageResponse<DoubtSummary> feed(CurrentUser me,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long subjectId,
                                           @RequestParam(required = false) Long topicId,
                                           @RequestParam(defaultValue = "false") boolean mine,
                                           @RequestParam(defaultValue = "false") boolean matchedToMe,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        DoubtStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = DoubtStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "status must be OPEN or SOLVED.");
            }
        }
        String query = q == null || q.isBlank() ? null : q.trim();
        if (query != null && (query.length() < 2 || query.length() > 100)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Search text must be 2 to 100 characters.");
        }
        return queries.feed(me, new DoubtQueryService.FeedFilter(parsed, subjectId, topicId, mine, matchedToMe, query),
                page, size);
    }

    @GetMapping("/{id}")
    public DoubtDetail detail(CurrentUser me, @PathVariable long id) {
        return queries.detail(me, id, true);
    }

    @PostMapping
    public ResponseEntity<DoubtDetail> create(CurrentUser me, @Valid @RequestBody CreateDoubtRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doubts.create(me, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(CurrentUser me, @PathVariable long id) {
        doubts.delete(me, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/similar")
    public SimilarResponse similar(CurrentUser me, @Valid @RequestBody SimilarRequest req) {
        return radar.similar(me, req);
    }

    @PostMapping("/deflections")
    public ResponseEntity<Void> deflect(CurrentUser me, @Valid @RequestBody DeflectionRequest req) {
        radar.recordDeflection(me, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/resolve-by-hint")
    public DoubtSummary resolveByHint(CurrentUser me, @PathVariable long id) {
        return doubts.resolveByHint(me, id);
    }
}
