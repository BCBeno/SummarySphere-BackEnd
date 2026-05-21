package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.dtos.ChatSchema;
import com.beno.summaryspherebackend.entities.ChatMessage;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.MessageRole;
import com.beno.summaryspherebackend.repositories.ChatMessageRepository;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.services.ChatService;
import com.beno.summaryspherebackend.services.DocumentVectorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVectorService documentVectorService;

    public ChatServiceImpl(ChatClient.Builder builder,
                           ChatMessageRepository chatMessageRepository,
                           DocumentRepository documentRepository,
                           DocumentVectorService documentVectorService) {
        this.chatClient = builder.build();
        this.chatMessageRepository = chatMessageRepository;
        this.documentRepository = documentRepository;
        this.documentVectorService = documentVectorService;
    }

    @Override
    @Transactional
    public ChatSchema.ChatMessageDTO sendMessage(String documentId, String message, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        List<String> relevantChunks = documentVectorService.searchRelevantChunks(documentId, message, 5);

        if (relevantChunks.isEmpty()) {
            throw new IllegalArgumentException("Document has no content to chat about.");
        }

        String contextContent = String.join("\n\n", relevantChunks);

        List<ChatMessage> recentMessages = chatMessageRepository.findTop10ByDocumentAndUserOrderByCreatedAtDesc(document, user);
        List<ChatMessage> history = new ArrayList<>(recentMessages);
        Collections.reverse(history);

        String systemContent = """
                You are a helpful assistant. The user wants to discuss the following document with you.
                Answer questions accurately and concisely based on the document content.
                Do NOT use MarkDown or asterisks (**), use only plain text. If you don't know the answer, say you don't know. Do NOT make up answers.

                DOCUMENT CONTENT:
                """ + contextContent;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

        for (ChatMessage msg : history) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(message));

        String aiResponse = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        if (aiResponse != null) {
            aiResponse = aiResponse.replaceAll("(?s)<think>.*?</think>\\s*", "");
            aiResponse = aiResponse.replaceAll("\\*\\*", "");
        }

        chatMessageRepository.save(new ChatMessage(null, document, user, MessageRole.USER, message, LocalDateTime.now()));
        ChatMessage assistantMsg = chatMessageRepository.save(
                new ChatMessage(null, document, user, MessageRole.ASSISTANT, aiResponse, LocalDateTime.now())
        );

        return new ChatSchema.ChatMessageDTO(MessageRole.ASSISTANT.name(), aiResponse, assistantMsg.getCreatedAt());
    }

    @Override
    public List<ChatSchema.ChatMessageDTO> getChatHistory(String documentId, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        return chatMessageRepository.findAllByDocumentAndUserOrderByCreatedAtAsc(document, user)
                .stream()
                .map(msg -> new ChatSchema.ChatMessageDTO(msg.getRole().name(), msg.getContent(), msg.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void clearChatHistory(String documentId, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        chatMessageRepository.deleteAllByDocumentAndUser(document, user);
    }
}
