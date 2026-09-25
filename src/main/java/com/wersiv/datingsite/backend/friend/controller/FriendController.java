package com.wersiv.datingsite.backend.friend.controller;

import com.wersiv.datingsite.backend.friend.dto.FriendshipResponse;
import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.service.FriendshipService;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipResponse>> getPendingRequests(@AuthenticationPrincipal UserPrincipal principal) {
        List<FriendshipResponse> responses = friendshipService.getPendingRequestsFor(principal.getId()).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public ResponseEntity<List<FriendshipResponse>> getFriends(@AuthenticationPrincipal UserPrincipal principal) {
        List<FriendshipResponse> responses = friendshipService.getAcceptedFriends(principal.getId()).stream()
                .map(FriendshipResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{userId}/accept")
    public ResponseEntity<FriendshipResponse> accept(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long userId) {
        Friendship friendship = friendshipService.accept(principal.getId(), userId);
        return ResponseEntity.ok(FriendshipResponse.from(friendship));
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<FriendshipResponse> block(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long userId) {
        Friendship friendship = friendshipService.block(principal.getId(), userId);
        return ResponseEntity.ok(FriendshipResponse.from(friendship));
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<FriendshipResponse> unblock(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable Long userId) {
        Friendship friendship = friendshipService.unblock(principal.getId(), userId);
        return ResponseEntity.ok(FriendshipResponse.from(friendship));
    }
}
