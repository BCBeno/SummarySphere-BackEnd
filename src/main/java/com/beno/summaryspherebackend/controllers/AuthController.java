package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.AuthSchema;
import com.beno.summaryspherebackend.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthSchema.AuthResponse> register(@RequestBody AuthSchema.RegisterRequest request) {
        AuthSchema.AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSchema.AuthResponse> login(@RequestBody AuthSchema.LoginRequest request) {
        AuthSchema.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

