package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.ChatSchema;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/documents")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/chat")
    public ResponseEntity<?> chat(
            @PathVariable String id,
            @Valid @RequestBody ChatSchema.ChatRequest request,
            @AuthenticationPrincipal User currentUser
    ) {

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

        return ResponseEntity.ok(chatService.getChatHistory(id, currentUser));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}/chat")
    public ResponseEntity<?> clearChat(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser
    ) {

        chatService.clearChatHistory(id, currentUser);
        return ResponseEntity.ok("Chat history cleared.");
    }

}
