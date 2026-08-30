package com.beno.summaryspherebackend.dtos;

public class AuthSchema {

    public record RegisterRequest(
            String fullName,
            String email,
            String password
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record AuthResponse(
            String token,
            String email,
            String fullName,
            String role,
            boolean emailVerified
    ) {}

    public record EmailVerificationRequest(
            String token
    ) {}

    public record EmailVerificationResponse(
            String message,
            boolean emailVerified
    ) {}


    public record ForgotPasswordRequest (
            String email
    ) {}

    public record ResetPasswordRequest(
            String token,
            String newPassword
    ) {}
}

