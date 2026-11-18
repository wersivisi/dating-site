package com.wersiv.datingsite.backend.message.dto;

import java.time.LocalDateTime;

public record ConversationResponse(Long userId, String displayName, String avatarUrl, String lastMessage,
                                   LocalDateTime lastMessageAt, String status, boolean canSend) {
}
