package com.beno.summaryspherebackend.services;

import java.util.concurrent.CompletableFuture;

public interface GeminiService {
    CompletableFuture<String> summarizeAsync(String docId, String type);
}