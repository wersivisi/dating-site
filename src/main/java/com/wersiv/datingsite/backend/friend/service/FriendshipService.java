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

@Service
@Transactional
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public Friendship authorizeMessage(Long senderId, Long receiverId) {
        rejectSelf(senderId, receiverId);
        Friendship friendship = friendshipRepository.findBetween(senderId, receiverId).orElse(null);

        if (friendship == null) {
            Friendship created = new Friendship(senderId, receiverId, FriendshipStatus.PENDING);
            return friendshipRepository.save(created);
        }

        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new CannotMessageException("error.friend.blocked");
        }

        if (friendship.getStatus() == FriendshipStatus.PENDING
                && friendship.getRequesterId().equals(senderId)) {
            throw new CannotMessageException("error.friend.awaiting_acceptance");
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
        rejectSelf(currentUserId, otherUserId);
        Friendship friendship = requireBetween(currentUserId, otherUserId);
        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new CannotMessageException("error.friend.blocked");
        }
        if (!friendship.getAddresseeId().equals(currentUserId)) {
            throw new CannotMessageException("error.friend.only_addressee_can_accept");
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return friendship;
    }

    public Friendship block(Long currentUserId, Long otherUserId) {
        rejectSelf(currentUserId, otherUserId);
        Friendship friendship = friendshipRepository.findBetween(currentUserId, otherUserId)
                .orElseGet(() -> new Friendship(currentUserId, otherUserId, FriendshipStatus.PENDING));
        if (friendship.getStatus() == FriendshipStatus.BLOCKED && !currentUserId.equals(friendship.getBlockedBy())) {
            throw new CannotMessageException("error.friend.blocked");
        }
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendship.setBlockedBy(currentUserId);
        return friendshipRepository.save(friendship);
    }

    public Friendship unblock(Long currentUserId, Long otherUserId) {
        rejectSelf(currentUserId, otherUserId);
        Friendship friendship = requireBetween(currentUserId, otherUserId);
        if (friendship.getStatus() != FriendshipStatus.BLOCKED
                || !currentUserId.equals(friendship.getBlockedBy())) {
            throw new CannotMessageException("error.friend.not_blocked_by_you");
        }
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship.setBlockedBy(null);
        return friendship;
    }

    public List<Friendship> getRelationships(Long userId) {
        return friendshipRepository.findByRequesterIdOrAddresseeId(userId, userId);
    }

    public Friendship like(Long userId, Long otherId) {
        rejectSelf(userId, otherId);
        Friendship existing = friendshipRepository.findBetween(userId, otherId).orElse(null);
        if (existing != null && existing.getStatus() != FriendshipStatus.BLOCKED
                && existing.getRequesterId().equals(userId)) {
            return existing;
        }
        return authorizeMessage(userId, otherId);
    }

    private void rejectSelf(Long userId, Long otherId) {
        if (userId.equals(otherId)) {
            throw new CannotMessageException("error.friend.self_interaction");
        }
    }

    private Friendship requireBetween(Long userA, Long userB) {
        return friendshipRepository.findBetween(userA, userB)
                .orElseThrow(() -> new FriendshipNotFoundException(userB));
    }
}
