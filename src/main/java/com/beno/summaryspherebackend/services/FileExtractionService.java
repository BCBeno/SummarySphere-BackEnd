package com.beno.summaryspherebackend.services;

public interface FileExtractionService {
    String extractTextFromBytes(byte[] bytes) throws Exception;
}
