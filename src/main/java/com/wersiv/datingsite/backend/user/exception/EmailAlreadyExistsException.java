package com.wersiv.datingsite.backend.user.exception;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(@NotBlank @Email String email) {
        super(email + " already exists");
    }
}
