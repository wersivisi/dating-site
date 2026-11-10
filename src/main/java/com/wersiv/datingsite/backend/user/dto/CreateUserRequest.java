package com.wersiv.datingsite.backend.user.dto;

import jakarta.validation.constraints.Email;
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
public class CreateUserRequest {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.username.required}")
    @Size(min = 4, max = 100, message = "{validation.username.size}")
    private String username;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 4, max = 255, message = "{validation.password.size}")
    private String password;
}
