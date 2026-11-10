package com.wersiv.datingsite.backend.friend.exception;

import com.wersiv.datingsite.backend.common.exception.LocalizedException;
import org.springframework.http.HttpStatus;

public class FriendshipNotFoundException extends LocalizedException {
    public FriendshipNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, "error.friend.relationship_not_found", userId);
    }
}
