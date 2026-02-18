package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.GeminiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GeminiServiceImpl implements GeminiService {
    private final ChatClient chatClient;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentRepository documentRepository;
    public GeminiServiceImpl(ChatClient.Builder builder, DocumentSummaryRepository documentSummaryRepository, DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
        this.documentSummaryRepository = documentSummaryRepository;
        this.chatClient = builder.build();
    }

    @Async
    @Override
    public String summarizeAsync(String docId, String type) {
        Document document = documentRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("Document with ID " + docId + " not found"));
        String rawText = document.getContent();

        if(rawText == null || rawText.isEmpty()) {
            throw new IllegalArgumentException("Text to summarize cannot be null or empty");
        }

        List<DocumentSummary> existingSummaries = documentSummaryRepository.findAllByDocument(document);
        existingSummaries.stream()
                .filter(summary -> summary.getSummaryType().equalsIgnoreCase(type))
                .findFirst()
                .ifPresent(summary -> {
                    throw new IllegalStateException("Summary of type '" + type + "' already exists for document ID: " + docId);
                });

        String prompt = """
            You are a professional editor. 
            Summarize the following text in a {type} style.
            Do not use MARKUP languages or special characters.
            
            TEXT TO SUMMARIZE:
            {text}
            """;

        String result = chatClient.prompt()
                .user(u -> u.text(prompt)
                        .param("type", type)
                        .param("text", rawText))
                .call()
                .content();
        documentSummaryRepository.save(new DocumentSummary(null, document, type, result, SummaryStatus.COMPLETED, LocalDateTime.now()));
        document.setStatus(SummaryStatus.COMPLETED.name());
        documentRepository.save(document);
        return result;
    }
}

