package com.wersiv.datingsite.backend.friend.service;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import com.wersiv.datingsite.backend.friend.exception.CannotMessageException;
import com.wersiv.datingsite.backend.friend.exception.FriendshipNotFoundException;
import com.wersiv.datingsite.backend.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FriendshipServiceTest {
    private FriendshipRepository repository;
    private FriendshipService service;

    @BeforeEach
    void setUp() {
        repository = mock(FriendshipRepository.class);
        service = new FriendshipService(repository);
    }

    @Test
    void firstMessageCreatesPendingRequest() {
        when(repository.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(repository.save(any(Friendship.class))).thenAnswer(call -> call.getArgument(0));
        Friendship result = service.authorizeMessage(1L, 2L);
        assertEquals(1L, result.getRequesterId());
        assertEquals(2L, result.getAddresseeId());
        assertEquals(FriendshipStatus.PENDING, result.getStatus());
        verify(repository).save(result);
    }

    @Test
    void requesterCannotSendAgainUntilAccepted() {
        Friendship relationship = relationship(FriendshipStatus.PENDING);
        assertDenied("error.friend.awaiting_acceptance", () -> service.authorizeMessage(1L, 2L));
        assertEquals(FriendshipStatus.PENDING, relationship.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void recipientReplyAcceptsPendingRequest() {
        Friendship relationship = relationship(FriendshipStatus.PENDING);
        assertSame(relationship, service.authorizeMessage(2L, 1L));
        assertEquals(FriendshipStatus.ACCEPTED, relationship.getStatus());
    }

    @Test
    void acceptedUsersCanBothSend() {
        Friendship relationship = relationship(FriendshipStatus.ACCEPTED);
        assertSame(relationship, service.authorizeMessage(1L, 2L));
        assertSame(relationship, service.authorizeMessage(2L, 1L));
    }

    @Test
    void blockedUsersCannotSendInEitherDirection() {
        Friendship relationship = relationship(FriendshipStatus.BLOCKED);
        relationship.setBlockedBy(1L);
        assertDenied("error.friend.blocked", () -> service.authorizeMessage(1L, 2L));
        assertDenied("error.friend.blocked", () -> service.authorizeMessage(2L, 1L));
        assertEquals(FriendshipStatus.BLOCKED, relationship.getStatus());
    }

    @Test
    void recipientCanExplicitlyAcceptPendingRequest() {
        Friendship relationship = relationship(FriendshipStatus.PENDING);
        assertSame(relationship, service.accept(2L, 1L));
        assertEquals(FriendshipStatus.ACCEPTED, relationship.getStatus());
        service.authorizeMessage(1L, 2L);
    }

    @Test
    void requesterCannotAcceptOwnRequest() {
        Friendship relationship = relationship(FriendshipStatus.PENDING);
        assertDenied("error.friend.only_addressee_can_accept", () -> service.accept(1L, 2L));
        assertEquals(FriendshipStatus.PENDING, relationship.getStatus());
    }

    @Test
    void acceptanceCannotOverrideBlockByEitherParticipant() {
        Friendship relationship = relationship(FriendshipStatus.BLOCKED);
        for (Long blocker : new Long[]{1L, 2L}) {
            relationship.setBlockedBy(blocker);
            assertDenied("error.friend.blocked", () -> service.accept(2L, 1L));
            assertEquals(FriendshipStatus.BLOCKED, relationship.getStatus());
            assertEquals(blocker, relationship.getBlockedBy());
        }
    }

    @Test
    void repeatedAcceptanceByRecipientIsIdempotent() {
        Friendship relationship = relationship(FriendshipStatus.ACCEPTED);
        assertSame(relationship, service.accept(2L, 1L));
        assertEquals(FriendshipStatus.ACCEPTED, relationship.getStatus());
    }

    @Test
    void eitherParticipantCanBlockAnAcceptedRelationship() {
        for (Long blocker : new Long[]{1L, 2L}) {
            Friendship relationship = relationship(FriendshipStatus.ACCEPTED);
            when(repository.save(relationship)).thenReturn(relationship);
            service.block(blocker, blocker.equals(1L) ? 2L : 1L);
            assertEquals(FriendshipStatus.BLOCKED, relationship.getStatus());
            assertEquals(blocker, relationship.getBlockedBy());
        }
    }

    @Test
    void blockedParticipantCannotTakeOverBlockAndUnblockIt() {
        Friendship relationship = relationship(FriendshipStatus.BLOCKED);
        relationship.setBlockedBy(1L);
        assertDenied("error.friend.blocked", () -> service.block(2L, 1L));
        assertDenied("error.friend.not_blocked_by_you", () -> service.unblock(2L, 1L));
        assertEquals(1L, relationship.getBlockedBy());
        assertEquals(FriendshipStatus.BLOCKED, relationship.getStatus());
    }

    @Test
    void blockerCanUnblockButRelationshipNeedsAcceptanceAgain() {
        Friendship relationship = relationship(FriendshipStatus.BLOCKED);
        relationship.setBlockedBy(1L);
        service.unblock(1L, 2L);
        assertEquals(FriendshipStatus.PENDING, relationship.getStatus());
        assertNull(relationship.getBlockedBy());
        assertDenied("error.friend.awaiting_acceptance", () -> service.authorizeMessage(1L, 2L));
        service.accept(2L, 1L);
        service.authorizeMessage(1L, 2L);
    }

    @Test
    void cannotUnblockAnUnblockedRelationship() {
        relationship(FriendshipStatus.ACCEPTED);
        assertDenied("error.friend.not_blocked_by_you", () -> service.unblock(1L, 2L));
    }

    @Test
    void selfMessagingAndRelationshipActionsAreRejectedBeforeRepositoryAccess() {
        assertDenied("error.friend.self_interaction", () -> service.authorizeMessage(1L, 1L));
        assertDenied("error.friend.self_interaction", () -> service.accept(1L, 1L));
        assertDenied("error.friend.self_interaction", () -> service.block(1L, 1L));
        assertDenied("error.friend.self_interaction", () -> service.unblock(1L, 1L));
        verifyNoInteractions(repository);
    }

    @Test
    void missingRequestCannotBeAccepted() {
        when(repository.findBetween(2L, 1L)).thenReturn(Optional.empty());
        assertThrows(FriendshipNotFoundException.class, () -> service.accept(2L, 1L));
    }

    @Test
    void canBlockWithoutAnExistingRelationship() {
        when(repository.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(repository.save(any(Friendship.class))).thenAnswer(call -> call.getArgument(0));
        Friendship result = service.block(1L, 2L);
        assertEquals(FriendshipStatus.BLOCKED, result.getStatus());
        assertEquals(1L, result.getBlockedBy());
    }

    private Friendship relationship(FriendshipStatus status) {
        Friendship relationship = new Friendship(1L, 2L, status);
        when(repository.findBetween(1L, 2L)).thenReturn(Optional.of(relationship));
        when(repository.findBetween(2L, 1L)).thenReturn(Optional.of(relationship));
        return relationship;
    }

    private void assertDenied(String code, Runnable action) {
        CannotMessageException exception = assertThrows(CannotMessageException.class, action::run);
        assertEquals(code, exception.getCode());
        assertEquals(403, exception.getStatus().value());
    }
}
