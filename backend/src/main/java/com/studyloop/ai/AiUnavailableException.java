package com.studyloop.ai;

public class AiUnavailableException extends RuntimeException {

    private final String code;

    public AiUnavailableException(String code) {
        super(code);
        this.code = code;
    }

    public AiUnavailableException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
