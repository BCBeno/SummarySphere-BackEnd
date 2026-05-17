package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.AIService;
import com.beno.summaryspherebackend.services.DocumentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AIServiceImpl implements AIService {
    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);
    private final ChatClient chatClient;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final BlobContainerClient blobContainerClient;

    public AIServiceImpl(ChatClient.Builder builder,
            DocumentSummaryRepository documentSummaryRepository,
            DocumentService documentService,
            DocumentRepository documentRepository,
            BlobContainerClient blobContainerClient) {
        this.documentSummaryRepository = documentSummaryRepository;
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        this.blobContainerClient = blobContainerClient;
        this.chatClient = builder.build();
    }

    @Async
    @Override
    public CompletableFuture<String> summarizeAsync(String docId, String type) {
        Document document = documentService.getDocumentById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document with ID " + docId + " not found"));
        String rawText = document.getContent();

        if (rawText == null || rawText.isEmpty()) {
            throw new IllegalArgumentException("Text to summarize cannot be null or empty");
        }

        List<DocumentSummary> existingSummaries = documentSummaryRepository.findAllByDocument(document);
        boolean hasActiveSummary = existingSummaries.stream()
                .filter(summary -> summary.getSummaryType().equalsIgnoreCase(type))
                .anyMatch(summary -> summary.getStatus() != SummaryStatus.FAILED);

        if (hasActiveSummary) {
            throw new IllegalStateException(
                    "Summary of type '" + type + "' already exists for document ID: " + docId);
        }

        // Clean up any failed summaries of this type to avoid cluttering the database
        List<DocumentSummary> failedSummaries = existingSummaries.stream()
                .filter(summary -> summary.getSummaryType().equalsIgnoreCase(type)
                        && summary.getStatus() == SummaryStatus.FAILED)
                .toList();
        if (!failedSummaries.isEmpty()) {
            documentSummaryRepository.deleteAll(failedSummaries);
        }

        String prompt = """
                You are a professional editor.
                Summarize the following text in a {type} style.
                Do not use MARKUP languages, asterisks (**), or special characters.
                Use the language of the document.

                TEXT TO SUMMARIZE:
                {text}
                """;

        try {
            int textLen = rawText == null ? 0 : rawText.length();
            log.debug("Summarize request: docId={}, type={}, textLen={}", docId, type, textLen);
            String result = chatClient.prompt()
                    .user(u -> u.text(prompt)
                            .param("type", type)
                            .param("text", rawText))
                    .call()
                    .content();

            if (result != null) {
                result = result.replaceAll("(?s)<think>.*?</think>\\s*", "");
                result = result.replaceAll("\\*\\*", "");
            }

            String blobName = buildSummaryBlobName(docId, type);
            uploadSummaryText(blobName, result);

            documentSummaryRepository.save(new DocumentSummary(null, document, type, result, blobName,
                    SummaryStatus.COMPLETED, LocalDateTime.now()));
            document.setStatus(SummaryStatus.COMPLETED.name());
            documentRepository.save(document);
            return CompletableFuture.completedFuture(result);
        } catch (RuntimeException ex) {
            int textLen = rawText == null ? 0 : rawText.length();
            log.error("Failed to generate summary for docId={}, type={}, textLen={}", docId, type, textLen, ex);
            documentSummaryRepository.save(
                    new DocumentSummary(null, document, type, null, null, SummaryStatus.FAILED, LocalDateTime.now()));
            document.setStatus(SummaryStatus.FAILED.name());
            documentRepository.save(document);
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "Unable to generate the summary right now. Please verify the OpenRouter API key and try again.",
                    ex));
        }
    }

    private String buildSummaryBlobName(String documentId, String type) {
        String normalizedType = type == null ? "summary" : type.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        if (normalizedType.isBlank()) {
            normalizedType = "summary";
        }
        return "summaries/" + documentId + "/" + normalizedType + "-" + UUID.randomUUID() + ".txt";
    }

    private void uploadSummaryText(String blobName, String summaryText) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        byte[] summaryBytes = summaryText.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(summaryBytes)) {
            blobClient.upload(dataStream, summaryBytes.length, true);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to store the generated summary in blob storage.", e);
        }
    }
}
