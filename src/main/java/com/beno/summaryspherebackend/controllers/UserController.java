package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final DocumentService documentService;
    private final UserService userService;

    public UserController(DocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;

    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me/documents")
    public ResponseEntity<List<DocumentListDTO>> getMyDocuments(@AuthenticationPrincipal User currentUser) {
        List<DocumentListDTO> documents = documentService.listFilesByUser(currentUser);
        return ResponseEntity.ok(documents);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal User currentUser) {
        userService.deleteUserWithFiles(currentUser);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}

