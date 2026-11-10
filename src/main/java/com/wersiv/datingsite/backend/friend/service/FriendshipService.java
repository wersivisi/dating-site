package com.wersiv.datingsite.backend.friend.service;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import com.wersiv.datingsite.backend.friend.exception.CannotMessageException;
import com.wersiv.datingsite.backend.friend.exception.FriendshipNotFoundException;
import com.wersiv.datingsite.backend.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * "Friends" access control for messaging. Two accepted users can message freely;
 * a first message to a stranger creates a PENDING relationship that the recipient
 * must accept or block.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public Friendship authorizeMessage(Long senderId, Long receiverId) {
        Friendship friendship = friendshipRepository.findBetween(senderId, receiverId).orElse(null);

        if (friendship == null) {
            Friendship created = new Friendship(senderId, receiverId, FriendshipStatus.PENDING);
            return friendshipRepository.save(created);
        }

        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new CannotMessageException("error.friend.blocked");
        }

        if (friendship.getStatus() == FriendshipStatus.PENDING
                && friendship.getAddresseeId().equals(senderId)) {

            friendship.setStatus(FriendshipStatus.ACCEPTED);
        }

        return friendship;
    }

    public List<Friendship> getPendingRequestsFor(Long userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING);
    }

    public List<Friendship> getAcceptedFriends(Long userId) {
        return friendshipRepository.findAcceptedFor(userId);
    }

    public Friendship accept(Long currentUserId, Long otherUserId) {
        Friendship friendship = requireBetween(currentUserId, otherUserId);
        if (!friendship.getAddresseeId().equals(currentUserId)) {
            throw new CannotMessageException("error.friend.only_addressee_can_accept");
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return friendship;
    }

    public Friendship block(Long currentUserId, Long otherUserId) {
        Friendship friendship = friendshipRepository.findBetween(currentUserId, otherUserId)
                .orElseGet(() -> new Friendship(currentUserId, otherUserId, FriendshipStatus.PENDING));
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship.setBlockedBy(currentUserId);
        return friendshipRepository.save(friendship);
    }

    public Friendship unblock(Long currentUserId, Long otherUserId) {
        Friendship friendship = requireBetween(currentUserId, otherUserId);
        if (friendship.getStatus() != FriendshipStatus.BLOCKED
                || !currentUserId.equals(friendship.getBlockedBy())) {
            throw new CannotMessageException("error.friend.not_blocked_by_you");
        }
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship.setBlockedBy(null);
        return friendship;
    }

    private Friendship requireBetween(Long userA, Long userB) {
        return friendshipRepository.findBetween(userA, userB)
                .orElseThrow(() -> new FriendshipNotFoundException(userB));
    }
}
