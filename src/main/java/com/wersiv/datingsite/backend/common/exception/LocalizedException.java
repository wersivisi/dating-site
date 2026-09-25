package com.wersiv.datingsite.backend.common.exception;

import org.springframework.http.HttpStatus;

public abstract class LocalizedException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    protected LocalizedException(HttpStatus status, String code, Object... args) {
        super(code);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
