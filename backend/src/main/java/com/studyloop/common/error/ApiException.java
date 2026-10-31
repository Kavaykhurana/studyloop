package com.studyloop.common.error;

import org.springframework.http.HttpHeaders;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Integer retryAfterSeconds;
    private final HttpHeaders headers = new HttpHeaders();

    public ApiException(ErrorCode code) {
        this(code, null, null);
    }

    public ApiException(ErrorCode code, String detailOverride) {
        this(code, detailOverride, null);
    }

    public ApiException(ErrorCode code, String detailOverride, Integer retryAfterSeconds) {
        super(detailOverride != null ? detailOverride : code.defaultDetail());
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ApiException withHeader(String name, String value) {
        headers.add(name, value);
        return this;
    }

    public ErrorCode code() {
        return code;
    }

    public Integer retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public HttpHeaders headers() {
        return headers;
    }
}
