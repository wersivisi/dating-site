package com.wersiv.datingsite.backend.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "{validation.receiverId.required}")
    private Long receiverId;

    @NotBlank(message = "{validation.content.required}")
    @Size(max = 4000, message = "{validation.content.size}")
    private String content;
}
