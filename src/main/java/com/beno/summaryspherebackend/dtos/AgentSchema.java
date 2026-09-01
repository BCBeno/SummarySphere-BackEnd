package com.beno.summaryspherebackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class AgentSchema {

    private AgentSchema() {}


    public record ChatRequest(
            @NotBlank(message = "Message is required")
            @Size(max = 4000, message = "Message must not exceed 4000 characters")
            String message
    ) {}


    public record MessageDTO(
            String role,
            String content,
            LocalDateTime createdAt
    ) {}

    public record ChatResponseDTO(
            String conversationId,
            MessageDTO message
    ) {}

    public record AgentChatDTO(
            String conversationId,
            LocalDateTime createdAt,
            LocalDateTime lastMessageAt,
            List<MessageDTO> messages
    ) {}
}
