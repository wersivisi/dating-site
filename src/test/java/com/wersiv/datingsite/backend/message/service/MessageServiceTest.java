package com.wersiv.datingsite.backend.message.service;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import com.wersiv.datingsite.backend.friend.exception.CannotMessageException;
import com.wersiv.datingsite.backend.friend.repository.FriendshipRepository;
import com.wersiv.datingsite.backend.friend.service.FriendshipService;
import com.wersiv.datingsite.backend.message.dto.SendMessageRequest;
import com.wersiv.datingsite.backend.message.entity.Message;
import com.wersiv.datingsite.backend.message.repository.MessageRepository;
import com.wersiv.datingsite.backend.user.exception.UserNotFoundException;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessageServiceTest {
    private MessageRepository messages;
    private UserRepository users;
    private FriendshipRepository friendships;
    private MessageService service;

    @BeforeEach
    void setUp() {
        messages = mock(MessageRepository.class);
        users = mock(UserRepository.class);
        friendships = mock(FriendshipRepository.class);
        service = new MessageService(messages, users, new FriendshipService(friendships));
    }

    @Test
    void firstMessageIsSavedButSecondMessageIsDeniedUntilRecipientReplies() {
        when(users.existsById(2L)).thenReturn(true);
        when(users.existsById(1L)).thenReturn(true);
        when(friendships.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(friendships.save(any(Friendship.class))).thenAnswer(call -> {
            Friendship relationship = call.getArgument(0);
            when(friendships.findBetween(1L, 2L)).thenReturn(Optional.of(relationship));
            when(friendships.findBetween(2L, 1L)).thenReturn(Optional.of(relationship));
            return relationship;
        });
        when(messages.save(any(Message.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals("Hello", service.sendMessage(1L, new SendMessageRequest(2L, "Hello")).getContent());
        assertThrows(CannotMessageException.class,
                () -> service.sendMessage(1L, new SendMessageRequest(2L, "Again")));
        verify(messages, times(1)).save(any(Message.class));

        service.sendMessage(2L, new SendMessageRequest(1L, "Hi"));
        service.sendMessage(1L, new SendMessageRequest(2L, "Thanks"));
        verify(messages, times(3)).save(any(Message.class));
    }

    @Test
    void blockedMessageIsNeverSaved() {
        when(users.existsById(2L)).thenReturn(true);
        when(friendships.findBetween(1L, 2L))
                .thenReturn(Optional.of(new Friendship(1L, 2L, FriendshipStatus.BLOCKED)));
        assertThrows(CannotMessageException.class,
                () -> service.sendMessage(1L, new SendMessageRequest(2L, "Hello")));
        verifyNoInteractions(messages);
    }

    @Test
    void selfMessageIsNeverSaved() {
        when(users.existsById(1L)).thenReturn(true);
        assertThrows(CannotMessageException.class,
                () -> service.sendMessage(1L, new SendMessageRequest(1L, "Hello")));
        verifyNoInteractions(messages, friendships);
    }

    @Test
    void nonexistentRecipientCreatesNeitherMessageNorRequest() {
        when(users.existsById(2L)).thenReturn(false);
        assertThrows(UserNotFoundException.class,
                () -> service.sendMessage(1L, new SendMessageRequest(2L, "Hello")));
        verifyNoInteractions(messages, friendships);
    }
}
