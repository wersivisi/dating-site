package com.wersiv.datingsite.backend.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

    @NotBlank(message = "{validation.handle.required}")
    @Size(min = 3, max = 32, message = "{validation.handle.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{validation.handle.pattern}")
    private String handle;

    @NotBlank(message = "{validation.displayName.required}")
    @Size(min = 1, max = 100, message = "{validation.displayName.size}")
    private String displayName;

    @Size(max = 2000, message = "{validation.bio.size}")
    private String bio;

    @Size(max = 2000, message = "{validation.preferences.size}")
    private String preferences;
}
