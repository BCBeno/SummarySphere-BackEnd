package com.beno.summaryspherebackend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class AgentSchema {

    private AgentSchema() {}


    public record ChatRequest(String message) {}


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
