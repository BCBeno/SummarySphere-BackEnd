package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.dtos.ChatSchema;
import com.beno.summaryspherebackend.entities.ChatMessage;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.MessageRole;
import com.beno.summaryspherebackend.repositories.ChatMessageRepository;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatPersistenceService {
    private final DocumentRepository documentRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatPersistenceService(DocumentRepository documentRepository, ChatMessageRepository chatMessageRepository) {
        this.documentRepository = documentRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatContext prepareChat(String documentId, String message, User user) {
        Document document = documentRepository.findByDocumentIdAndUploadedById(documentId, user == null ? null : user.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Document not found."));

        // Keep the previous messages separate; callAi adds the current message once.
        List<ChatMessage> history = new ArrayList<>(
                chatMessageRepository.findTop10ByDocumentAndUserOrderByCreatedAtDesc(document, user));
        Collections.reverse(history);
        chatMessageRepository.save(new ChatMessage(null, document, user, MessageRole.USER, message, LocalDateTime.now()));
        return new ChatContext(document, user, message, history);
    }

    @Transactional
    public ChatSchema.ChatMessageDTO saveAssistantResponse(ChatContext context, String aiResponse) {
        ChatMessage saved = chatMessageRepository.save(new ChatMessage(null, context.document(), context.user(),
                MessageRole.ASSISTANT, aiResponse, LocalDateTime.now()));
        return new ChatSchema.ChatMessageDTO(MessageRole.ASSISTANT.name(), aiResponse, saved.getCreatedAt());
    }

    public record ChatContext(Document document, User user, String message, List<ChatMessage> history) {}
}
