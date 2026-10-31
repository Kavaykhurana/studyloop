package com.studyloop.common.error;

import com.studyloop.ai.AiUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest req) {
        ProblemDetail body = problem(ex.code(), ex.getMessage(), req);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.code().httpStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (ex.retryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()));
        }
        builder.headers(ex.headers());
        return builder.body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(Map.of("field", fe.getField(), "message",
                    fe.getDefaultMessage() == null ? "is invalid" : fe.getDefaultMessage()));
        }
        ex.getBindingResult().getGlobalErrors().forEach(ge -> errors.add(Map.of("field", ge.getObjectName(),
                "message", ge.getDefaultMessage() == null ? "is invalid" : ge.getDefaultMessage())));
        return validation(errors, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            errors.add(Map.of("field", path.substring(path.lastIndexOf('.') + 1), "message", v.getMessage()));
        });
        return validation(errors, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex,
                                                                HttpServletRequest req) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(r -> r.getResolvableErrors().forEach(e -> errors.add(Map.of(
                "field", r.getMethodParameter().getParameterName() == null ? "parameter"
                        : r.getMethodParameter().getParameterName(),
                "message", e.getDefaultMessage() == null ? "is invalid" : e.getDefaultMessage()))));
        return validation(errors, req);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ProblemDetail> handleBadParam(Exception ex, HttpServletRequest req) {
        return respond(ErrorCode.BAD_REQUEST, null, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return respond(ErrorCode.MALFORMED_JSON, null, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return respond(ErrorCode.FILE_TOO_LARGE, null, req);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleAi(AiUnavailableException ex, HttpServletRequest req) {
        log.warn("AI unavailable: {}", ex.getCode());
        return respond(ErrorCode.AI_UNAVAILABLE, null, req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(ErrorCode.CONFLICT, null, req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException ex,
                                                      HttpServletRequest req) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, null, req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest req) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return respond(ErrorCode.NOT_FOUND, null, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception [traceId={}]", MDC.get(TraceIdFilter.MDC_KEY), ex);
        return respond(ErrorCode.INTERNAL_ERROR, null, req);
    }

    private ResponseEntity<ProblemDetail> validation(List<Map<String, String>> errors, HttpServletRequest req) {
        String detail = errors.size() == 1 ? "1 field is invalid." : errors.size() + " fields are invalid.";
        ProblemDetail body = problem(ErrorCode.VALIDATION_FAILED, detail, req);
        body.setProperty("errors", errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorCode code, String detail, HttpServletRequest req) {
        return ResponseEntity.status(code.httpStatus()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem(code, detail, req));
    }

    private ProblemDetail problem(ErrorCode code, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.httpStatus(),
                detail != null ? detail : code.defaultDetail());
        pd.setType(URI.create(code.typeUri()));
        pd.setTitle(code.title());
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("code", code.name());
        pd.setProperty("traceId", MDC.get(TraceIdFilter.MDC_KEY));
        return pd;
    }
}
