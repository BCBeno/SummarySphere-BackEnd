package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.dtos.AuthSchema;
import com.beno.summaryspherebackend.exceptions.RateLimitExceededException;
import com.beno.summaryspherebackend.services.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.beno.summaryspherebackend.entities.User;

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

    @PostMapping("/email-verification/request")
    public ResponseEntity<AuthSchema.EmailVerificationResponse> requestEmailVerification(
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthSchema.EmailVerificationResponse("Authentication required", false));
        }
        authService.requestEmailVerification(user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(new AuthSchema.EmailVerificationResponse(
                "Verification email sent", false));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<AuthSchema.EmailVerificationResponse> confirmEmailVerification(
            @RequestBody AuthSchema.EmailVerificationRequest request) {
        authService.confirmEmailVerification(request.token());
        return ResponseEntity.ok(new AuthSchema.EmailVerificationResponse(
                "Email verified successfully", true));
    }

}

