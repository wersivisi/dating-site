package com.wersiv.datingsite.backend.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String username;
    private String status;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
}
