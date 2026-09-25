package com.wersiv.datingsite.backend.message.service;

import com.wersiv.datingsite.backend.friend.service.FriendshipService;
import com.wersiv.datingsite.backend.message.dto.MessageResponse;
import com.wersiv.datingsite.backend.message.dto.SendMessageRequest;
import com.wersiv.datingsite.backend.message.entity.Message;
import com.wersiv.datingsite.backend.message.repository.MessageRepository;
import com.wersiv.datingsite.backend.user.exception.UserNotFoundException;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FriendshipService friendshipService;

    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        if (!userRepository.existsById(request.getReceiverId())) {
            throw new UserNotFoundException(request.getReceiverId());
        }

        friendshipService.authorizeMessage(senderId, request.getReceiverId());

        Message message = new Message(senderId, request.getReceiverId(), request.getContent());
        Message saved = messageRepository.save(message);

        return MessageResponse.from(saved);
    }

    public List<MessageResponse> getConversation(Long currentUserId, Long otherUserId) {
        return messageRepository.findConversation(currentUserId, otherUserId).stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }
}
