package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.dtos.ChatSchema;
import com.beno.summaryspherebackend.entities.User;

import java.util.List;

public interface ChatService {
    ChatSchema.ChatMessageDTO sendMessage(String documentId, String message, User user);
    List<ChatSchema.ChatMessageDTO> getChatHistory(String documentId, User user);
    void clearChatHistory(String documentId, User user);
}
