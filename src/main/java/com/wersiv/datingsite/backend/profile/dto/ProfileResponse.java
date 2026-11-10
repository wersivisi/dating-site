package com.wersiv.datingsite.backend.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private Long userId;
    private String handle;
    private String displayName;
    private String bio;

    private java.time.LocalDate birthdate;

    private String city;

    private String gender;

    private String interests;

    private String goal;

    private String idealEvening;

    private String preferences;
    private String avatarUrl;
}
