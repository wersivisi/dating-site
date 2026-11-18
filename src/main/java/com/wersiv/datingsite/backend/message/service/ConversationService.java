package com.wersiv.datingsite.backend.message.service;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import com.wersiv.datingsite.backend.friend.exception.CannotMessageException;
import com.wersiv.datingsite.backend.friend.service.FriendshipService;
import com.wersiv.datingsite.backend.message.dto.*;
import com.wersiv.datingsite.backend.message.entity.Message;
import com.wersiv.datingsite.backend.message.repository.MessageRepository;
import com.wersiv.datingsite.backend.profile.repository.ProfileRepository;
import com.wersiv.datingsite.backend.user.exception.UserNotFoundException;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConversationService {
    private final FriendshipService friendships;
    private final MessageRepository messages;
    private final ProfileRepository profiles;
    private final UserRepository users;

    public List<ConversationResponse> list(Long userId) {
        Map<Long, Friendship> relationships = relationships(userId);
        Map<Long, Message> latest = new LinkedHashMap<>();
        for (Message message : messages.findForUser(userId)) {
            Long other = message.getSenderId().equals(userId) ? message.getReceiverId() : message.getSenderId();
            latest.putIfAbsent(other, message);
        }
        Set<Long> peers = new LinkedHashSet<>(latest.keySet());
        peers.addAll(relationships.keySet());
        return peers.stream().filter(id -> !id.equals(userId))
                .filter(id -> !blocked(relationships.get(id)))
                .map(id -> describe(userId, id, relationships.get(id), latest.get(id))).toList();
    }

    public ConversationState state(Long userId, Long otherId) {
        if (userId.equals(otherId)) throw new CannotMessageException("error.friend.self_interaction");
        Friendship relationship = relationships(userId).get(otherId);
        if (blocked(relationship)) throw new CannotMessageException("error.friend.blocked");
        ConversationResponse recipient = describe(userId, otherId, relationship, null);
        return new ConversationState(userId, recipient, messages.findConversation(userId, otherId).stream()
                .map(MessageResponse::from).toList());
    }

    private Map<Long, Friendship> relationships(Long userId) {
        Map<Long, Friendship> result = new LinkedHashMap<>();
        for (Friendship friendship : friendships.getRelationships(userId)) {
            result.put(friendship.getRequesterId().equals(userId) ? friendship.getAddresseeId() : friendship.getRequesterId(), friendship);
        }
        return result;
    }

    private boolean blocked(Friendship relationship) {
        return relationship != null && relationship.getStatus() == FriendshipStatus.BLOCKED;
    }

    private ConversationResponse describe(Long currentId, Long otherId, Friendship relationship, Message latest) {
        var user = users.findById(otherId).orElseThrow(() -> new UserNotFoundException(otherId));
        var profile = profiles.findById(otherId).orElse(null);
        String name = profile == null ? user.getUsername() : profile.getDisplayName();
        String avatar = profile == null || profile.getAvatarPath() == null ? null : "/avatars/" + profile.getAvatarPath();
        boolean canSend = relationship == null || relationship.getStatus() == FriendshipStatus.ACCEPTED
                || relationship.getStatus() == FriendshipStatus.PENDING && relationship.getAddresseeId().equals(currentId);
        return new ConversationResponse(otherId, name, avatar, latest == null ? null : latest.getContent(),
                latest == null ? null : latest.getSentAt(), relationship == null ? "NEW" : relationship.getStatus().name(), canSend);
    }
}
