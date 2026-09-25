package com.wersiv.datingsite.backend.user.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends LocalizedException {
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "error.user.email_taken", email);
    }
}
