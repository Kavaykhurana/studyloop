package com.studyloop.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed", "Some fields are invalid."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "Malformed JSON", "Request body is not valid JSON."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request", "The request could not be understood."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required", "Please sign in to continue."),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Bad credentials", "Email or password is incorrect."),
    REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token invalid", "Your session ended. Please sign in again."),
    REFRESH_REUSED(HttpStatus.UNAUTHORIZED, "Refresh token reused", "Your session ended. Please sign in again."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "You do not have access to this."),
    CSRF_HEADER_MISSING(HttpStatus.FORBIDDEN, "Missing header", "Required header X-Requested-With is missing."),
    CANNOT_ANSWER_OWN(HttpStatus.FORBIDDEN, "Cannot answer own doubt", "You cannot answer your own doubt."),
    NOT_DOUBT_OWNER(HttpStatus.FORBIDDEN, "Not the doubt owner", "Only the person who asked can do this."),
    DEMO_READ_ONLY(HttpStatus.FORBIDDEN, "Demo account", "Demo accounts cannot change name or email."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "That method is not supported here."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
            "Send the request body as application/json."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found", "We couldn't find that."),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email already registered", "An account with this email already exists."),
    DOUBT_SOLVED(HttpStatus.CONFLICT, "Doubt already solved", "This doubt is already solved."),
    ALREADY_ACCEPTED(HttpStatus.CONFLICT, "Already accepted", "An answer has already been accepted."),
    HAS_ANSWERS(HttpStatus.CONFLICT, "Has answers", "A doubt with answers cannot be deleted."),
    NO_HINTS_USED(HttpStatus.CONFLICT, "No hints used", "Unlock at least one hint first."),
    ALL_HINTS_UNLOCKED(HttpStatus.CONFLICT, "All hints unlocked", "You have unlocked every hint."),
    CONFLICT(HttpStatus.CONFLICT, "Conflict", "That conflicts with existing data."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", "Images must be under 4 MB."),
    NOT_ACADEMIC(HttpStatus.UNPROCESSABLE_ENTITY, "Not a study doubt",
            "StudyLoop is for study doubts. Rephrase this as a question about a subject."),
    SUBJECT_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Subject required",
            "Pick a subject so we can find the right seniors."),
    WEAK_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "Weak password",
            "Use 8+ characters with at least one letter and one number."),
    UNSAFE_CONTENT(HttpStatus.UNPROCESSABLE_ENTITY, "Unsafe content", "That content isn't allowed."),
    INVALID_EXAM_DATE(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid exam date",
            "Pick an exam date between tomorrow and 60 days from now."),
    IMAGE_UNREADABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Image unreadable",
            "We couldn't find a question in that photo. Try a closer, well-lit shot of just the problem."),
    UNSUPPORTED_IMAGE(HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported image", "Upload a JPG, PNG or WebP image."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limited", "Slow down a little — try again soon."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Something went wrong on our side."),
    AI_BAD_OUTPUT(HttpStatus.BAD_GATEWAY, "AI output unusable", "Couldn't build a sensible plan. Try again."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI unavailable",
            "The AI helper is busy right now. Try again in a moment.");

    private final HttpStatus httpStatus;
    private final String title;
    private final String defaultDetail;

    ErrorCode(HttpStatus httpStatus, String title, String defaultDetail) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String title() {
        return title;
    }

    public String defaultDetail() {
        return defaultDetail;
    }

    public String typeUri() {
        return "https://studyloop.app/errors/" + name().toLowerCase().replace('_', '-');
    }
}
