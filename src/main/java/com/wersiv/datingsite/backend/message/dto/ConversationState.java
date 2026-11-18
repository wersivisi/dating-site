package com.wersiv.datingsite.backend.message.dto;

import java.util.List;

public record ConversationState(Long currentUserId, ConversationResponse recipient, List<MessageResponse> messages) {
}
