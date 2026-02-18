package com.beno.summaryspherebackend.services;

public interface GeminiService {
    String summarizeAsync(String docId, String type);
}