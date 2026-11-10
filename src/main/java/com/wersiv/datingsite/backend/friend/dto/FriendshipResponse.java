package com.wersiv.datingsite.backend.friend.dto;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private Long requesterId;
    private Long addresseeId;
    private String status;

    public static FriendshipResponse from(Friendship friendship) {
        return new FriendshipResponse(
                friendship.getRequesterId(),
                friendship.getAddresseeId(),
                friendship.getStatus().name()
        );
    }
}
