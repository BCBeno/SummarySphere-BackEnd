package com.beno.summaryspherebackend.services;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    CompletableFuture<Void> sendResetPasswordEmail(String email, String token);
}
