package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.events.SummaryRequestedEvent;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.DocumentSummaryService;
import com.beno.summaryspherebackend.services.RateLimitService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentSummaryServiceImpl implements DocumentSummaryService {

    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentRepository documentRepository;
    private final BlobContainerClient blobContainerClient;
    private final RateLimitService rateLimitService;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentSummaryServiceImpl(DocumentSummaryRepository documentSummaryRepository,
                                      DocumentRepository documentRepository,
                                      BlobContainerClient blobContainerClient,
                                      RateLimitService rateLimitService,
                                      ApplicationEventPublisher eventPublisher) {
        this.documentSummaryRepository = documentSummaryRepository;
        this.documentRepository = documentRepository;
        this.blobContainerClient = blobContainerClient;
        this.rateLimitService = rateLimitService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DocumentSummary requestSummary(String documentId, String summaryType, User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("Authenticated user is required for summarization.");
        }

        String normalizedType = summaryType == null ? "" : summaryType.trim();
        if (normalizedType.isEmpty()) {
            throw new IllegalArgumentException("Summary type is required.");
        }

        Document document = documentRepository.findOwnedByIdForUpdate(documentId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Document not found."));

        rateLimitService.checkSummarization(currentUser.getId().toString());

        boolean alreadyExists = documentSummaryRepository
                .existsByDocumentAndSummaryTypeIgnoreCaseAndStatusIn(
                        document,
                        normalizedType,
                        List.of(SummaryStatus.PENDING, SummaryStatus.PROCESSING, SummaryStatus.COMPLETED));
        if (alreadyExists) {
            throw new IllegalStateException(
                    "Summary of type '" + normalizedType + "' already exists for document ID: " + documentId);
        }

        DocumentSummary savedSummary = createPendingSummary(document, normalizedType);
        document.setStatus(SummaryStatus.PENDING.name());

        eventPublisher.publishEvent(new SummaryRequestedEvent(savedSummary.getId()));
        return savedSummary;
    }

    public DocumentSummary createPendingSummary(Document document, String summaryType) {
        DocumentSummary summary = new DocumentSummary();
        summary.setDocument(document);
        summary.setSummaryType(summaryType);
        summary.setStatus(SummaryStatus.PENDING);
        summary.setCreatedAt(java.time.LocalDateTime.now());
        summary.setAttemptCount(0);
        return documentSummaryRepository.save(summary);
    }

    @Override
    public Optional<DocumentSummary> getLatestSummaryForDocument(String documentId, String userId) {
        Document document = documentRepository.findByDocumentIdAndUploadedById(documentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found."));
        return documentSummaryRepository.findFirstByDocumentOrderByCreatedAtDesc(document)
            .map(this::hydrateSummary);
    }

    @Override
    public Optional<DocumentSummary> getLatestSummaryForDocumentByType(String documentId, String summaryType, String userId) {
        Document document = documentRepository.findByDocumentIdAndUploadedById(documentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found."));
        if (summaryType == null || summaryType.trim().isEmpty()) {
            return Optional.empty();
        }
        return documentSummaryRepository.findFirstByDocumentAndSummaryTypeIgnoreCaseOrderByCreatedAtDesc(document, summaryType.trim())
        .map(this::hydrateSummary);
    }

    @Override
    public List<DocumentSummary> getSummariesForDocument(String documentId, String userId) {
        Document document = documentRepository.findByDocumentIdAndUploadedById(documentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found."));
        return documentSummaryRepository.findAllByDocumentOrderByCreatedAtDesc(document)
                .stream()
                .map(this::hydrateSummary)
                .toList();
    }

    private DocumentSummary hydrateSummary(DocumentSummary summary) {
        if (summary == null) {
            return null;
        }

        if (summary.getSummaryBlobName() != null && !summary.getSummaryBlobName().isBlank()) {
            BlobClient blobClient = blobContainerClient.getBlobClient(summary.getSummaryBlobName());
            if (blobClient.exists()) {
                summary.setSummaryText(readBlobAsText(blobClient));
                return summary;
            }

            if (summary.getStatus() == SummaryStatus.FAILED) {
                summary.setSummaryText("Summary generation failed.");
                return summary;
            }

            throw new IllegalStateException("Summary blob not found: " + summary.getSummaryBlobName());
        }

        if (summary.getStatus() == SummaryStatus.FAILED) {
            summary.setSummaryText("Summary generation failed.");
        }

        return summary;
    }

    private String readBlobAsText(BlobClient blobClient) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            blobClient.downloadStream(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the summary from blob storage.", e);
        }
    }
}

