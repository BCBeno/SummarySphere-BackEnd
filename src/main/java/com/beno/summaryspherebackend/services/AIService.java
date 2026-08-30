package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.User;

import java.util.concurrent.CompletableFuture;

public interface AIService {
    CompletableFuture<String> summarizeAsync(String docId, String type);

    CompletableFuture<String> summarizeAsync(String docId, String type, User user);
}