package com.wersiv.datingsite.backend.message.controller;

import com.wersiv.datingsite.backend.message.dto.MessageResponse;
import com.wersiv.datingsite.backend.message.dto.SendMessageRequest;
import com.wersiv.datingsite.backend.message.service.MessageService;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final com.wersiv.datingsite.backend.message.service.ConversationService conversations;

    @GetMapping("/conversations")
    public List<com.wersiv.datingsite.backend.message.dto.ConversationResponse> conversations(@AuthenticationPrincipal UserPrincipal principal) {
        return conversations.list(principal.getId());
    }

    @GetMapping("/with/{userId}/state")
    public com.wersiv.datingsite.backend.message.dto.ConversationState state(@AuthenticationPrincipal UserPrincipal principal,
                                                                           @PathVariable Long userId) {
        return conversations.state(principal.getId(), userId);
    }

    @PostMapping
    public ResponseEntity<MessageResponse> send(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.sendMessage(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/with/{userId}")
    public ResponseEntity<List<MessageResponse>> getConversation(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PathVariable Long userId) {
        return ResponseEntity.ok(conversations.state(principal.getId(), userId).messages());
    }
}
