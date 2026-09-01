package com.beno.summaryspherebackend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthSchema {

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            String fullName,
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
            @Pattern(
                    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                    message = "Password must contain uppercase, lowercase, number and special character"
            )
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            String email,
            @NotBlank(message = "Password is required")
            @Size(max = 128, message = "Password must not exceed 128 characters")
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
            @NotBlank(message = "Verification token is required")
            @Size(max = 512, message = "Verification token must not exceed 512 characters")
            String token
    ) {}

    public record EmailVerificationResponse(
            String message,
            boolean emailVerified
    ) {}


    public record ForgotPasswordRequest (
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Reset token is required")
            @Size(max = 512, message = "Reset token must not exceed 512 characters")
            String token,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
            @Pattern(
                    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                    message = "Password must contain uppercase, lowercase, number and special character"
            )
            String newPassword
    ) {}
}

