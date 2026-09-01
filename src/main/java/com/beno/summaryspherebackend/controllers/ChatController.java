package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.ChatSchema;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.services.ChatService;
import com.beno.summaryspherebackend.services.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class ChatController {

    private final ChatService chatService;
    private final DocumentService documentService;

    public ChatController(ChatService chatService, DocumentService documentService) {
        this.chatService = chatService;
        this.documentService = documentService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/chat")
    public ResponseEntity<?> chat(
            @PathVariable String id,
            @Valid @RequestBody ChatSchema.ChatRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Optional<Document> docOpt = documentService.getDocumentById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();
        if (!isOwner(docOpt.get(), currentUser)) {
            return ResponseEntity.status(403).body("You are not authorized to chat about this document.");
        }

        try {
            ChatSchema.ChatMessageDTO response = chatService.sendMessage(id, request.message(), currentUser);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}/chat")
    public ResponseEntity<?> getChatHistory(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        Optional<Document> docOpt = documentService.getDocumentById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();
        if (!isOwner(docOpt.get(), currentUser)) {
            return ResponseEntity.status(403).body("You are not authorized to view this chat history.");
        }

        return ResponseEntity.ok(chatService.getChatHistory(id, currentUser));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}/chat")
    public ResponseEntity<?> clearChat(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {
        Optional<Document> docOpt = documentService.getDocumentById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();
        if (!isOwner(docOpt.get(), currentUser)) {
            return ResponseEntity.status(403).body("You are not authorized to clear this chat history.");
        }

        chatService.clearChatHistory(id, currentUser);
        return ResponseEntity.ok("Chat history cleared.");
    }

    private boolean isOwner(Document doc, User currentUser) {
        if (doc == null || currentUser == null) return false;
        User owner = doc.getUploadedBy();
        if (owner == null) return false;
        return Objects.equals(owner.getId(), currentUser.getId());
    }
}
