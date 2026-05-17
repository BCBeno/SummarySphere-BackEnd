package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.AgentSchema;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.services.agent.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/chat")
    public ResponseEntity<AgentSchema.AgentChatDTO> getChat(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(agentService.getOrCreateChat(currentUser));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody AgentSchema.ChatRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body("Message cannot be empty.");
        }

        try {
            AgentSchema.ChatResponseDTO response = agentService.chat(request.message(), currentUser);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/chat")
    public ResponseEntity<String> clearChat(
            @AuthenticationPrincipal User currentUser
    ) {
        agentService.clearChat(currentUser);
        return ResponseEntity.ok("Chat cleared.");
    }
}
