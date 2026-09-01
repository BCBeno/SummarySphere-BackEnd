package com.beno.summaryspherebackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ChatSchema {

    public ChatSchema() {}

    public record ChatRequest(
            @NotBlank(message = "Message is required")
            @Size(max = 4000, message = "Message must not exceed 4000 characters")
            String message
    ) {}

    public record ChatMessageDTO(String role, String content, LocalDateTime createdAt) {}
}
