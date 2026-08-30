package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.AuthSchema;
import com.beno.summaryspherebackend.exceptions.RateLimitExceededException;
import com.beno.summaryspherebackend.services.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthSchema.AuthResponse> register(@RequestBody AuthSchema.RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthSchema.AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSchema.AuthResponse> login(@RequestBody AuthSchema.LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthSchema.AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
        public ResponseEntity<?> forgotPassword(@RequestBody AuthSchema.ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        // Email is sent asynchronously to avoid timeouts and return immediately
        try {
            authService.forgotPassword(request.email(), httpRequest.getRemoteAddr());
        } catch (RateLimitExceededException ex) {
            throw ex;
        } catch (Exception ignored) {
            // Intentionally return the same response to avoid leaking account/email state.
            // This prevents user enumeration attacks.
        }
        // Always return 200 - email sending is async and doesn't block the response
        return ResponseEntity.ok("Password reset link sent to your email");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody AuthSchema.ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok("Password reset successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}

