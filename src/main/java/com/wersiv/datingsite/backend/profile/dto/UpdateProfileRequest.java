package com.wersiv.datingsite.backend.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "{validation.displayName.required}")
    @Size(min = 1, max = 100, message = "{validation.displayName.size}")
    private String displayName;

    @Size(max = 2000, message = "{validation.bio.size}")
    private String bio;

    @Size(max = 2000, message = "{validation.preferences.size}")
    private String preferences;
}
