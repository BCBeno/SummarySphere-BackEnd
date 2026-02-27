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
            String role
    ) {}
}

