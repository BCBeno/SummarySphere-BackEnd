package com.beno.summaryspherebackend.services;

public interface EmailService {
    void sendResetPasswordEmail(String email, String token);
}
