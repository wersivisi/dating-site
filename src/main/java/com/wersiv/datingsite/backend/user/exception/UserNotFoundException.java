package com.wersiv.datingsite.backend.user.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends LocalizedException {
    public UserNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.user.not_found", id);
    }
}
