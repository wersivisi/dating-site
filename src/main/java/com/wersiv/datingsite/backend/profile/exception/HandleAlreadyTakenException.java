package com.wersiv.datingsite.backend.profile.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

public class HandleAlreadyTakenException extends LocalizedException {
    public HandleAlreadyTakenException(String handle) {
        super(HttpStatus.CONFLICT, "error.profile.handle_taken", handle);
    }
}
