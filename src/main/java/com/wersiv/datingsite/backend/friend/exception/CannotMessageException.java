package com.wersiv.datingsite.backend.friend.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;


public class CannotMessageException extends LocalizedException {
    public CannotMessageException(String code) {
        super(HttpStatus.FORBIDDEN, code);
    }
}
