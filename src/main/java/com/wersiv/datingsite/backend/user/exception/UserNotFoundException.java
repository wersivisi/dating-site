package com.wersiv.datingsite.backend.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super(id + " not found");
    }
}
