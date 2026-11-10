package com.wersiv.datingsite.backend.profile.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

public class ProfileAlreadyExistsException extends LocalizedException {
    public ProfileAlreadyExistsException(Long userId) {
        super(HttpStatus.CONFLICT, "error.profile.already_exists", userId);
    }
}
