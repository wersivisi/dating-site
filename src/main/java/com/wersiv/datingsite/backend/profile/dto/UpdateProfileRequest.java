package com.wersiv.datingsite.backend.profile.dto;

import com.wersiv.datingsite.backend.profile.validation.AdultBirthdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
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

    @NotNull(message = "{validation.birthdate.required}")
    @AdultBirthdate
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private java.time.LocalDate birthdate;

    @Size(max = 255)
    private String city;

    @Size(max = 255)
    private String gender;

    @Size(max = 255)
    private String interests;

    @Size(max = 255)
    private String goal;

    @Size(max = 255)
    private String idealEvening;

    @Size(max = 2000, message = "{validation.preferences.size}")
    private String preferences;
}
