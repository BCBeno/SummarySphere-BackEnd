package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.services.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final DocumentService documentService;

    public UserController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me/documents")
    public ResponseEntity<List<DocumentListDTO>> getMyDocuments(@AuthenticationPrincipal User currentUser) {
        List<DocumentListDTO> documents = documentService.listFilesByUser(currentUser);
        return ResponseEntity.ok(documents);
    }
}

