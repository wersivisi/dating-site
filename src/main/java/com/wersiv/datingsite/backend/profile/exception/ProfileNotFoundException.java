package com.wersiv.datingsite.backend.profile.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends LocalizedException {
    public ProfileNotFoundException(String identifier) {
        super(HttpStatus.NOT_FOUND, "error.profile.not_found", identifier);
    }
}
