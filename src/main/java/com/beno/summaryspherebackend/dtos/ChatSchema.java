package com.beno.summaryspherebackend.dtos;

import java.time.LocalDateTime;

public class ChatSchema {

    public ChatSchema() {}

    public record ChatRequest(String message) {}

    public record ChatMessageDTO(String role, String content, LocalDateTime createdAt) {}
}
