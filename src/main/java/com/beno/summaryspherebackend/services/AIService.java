package com.beno.summaryspherebackend.services;

import java.util.concurrent.CompletableFuture;

public interface AIService {
    CompletableFuture<String> summarizeAsync(String docId, String type);
}