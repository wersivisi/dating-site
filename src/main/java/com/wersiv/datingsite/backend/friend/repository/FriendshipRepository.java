package com.wersiv.datingsite.backend.friend.repository;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("select f from Friendship f where " +
            "(f.requesterId = :userA and f.addresseeId = :userB) or " +
            "(f.requesterId = :userB and f.addresseeId = :userA)")
    Optional<Friendship> findBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status);

    @Query("select f from Friendship f where f.status = com.wersiv.datingsite.backend.friend.entity.FriendshipStatus.ACCEPTED " +
            "and (f.requesterId = :userId or f.addresseeId = :userId)")
    List<Friendship> findAcceptedFor(@Param("userId") Long userId);
}
