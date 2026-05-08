package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.DocumentSummaryService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentSummaryServiceImpl implements DocumentSummaryService {

    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentRepository documentRepository;
    private final BlobContainerClient blobContainerClient;

    public DocumentSummaryServiceImpl(DocumentSummaryRepository documentSummaryRepository,
                                      DocumentRepository documentRepository,
                                      BlobContainerClient blobContainerClient) {
        this.documentSummaryRepository = documentSummaryRepository;
        this.documentRepository = documentRepository;
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public Optional<DocumentSummary> getLatestSummaryForDocument(String documentId) {
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return Optional.empty();
        }
        Document document = docOpt.get();
        return documentSummaryRepository.findFirstByDocumentOrderByCreatedAtDesc(document)
            .map(this::hydrateSummary);
    }

    @Override
    public Optional<DocumentSummary> getLatestSummaryForDocumentByType(String documentId, String summaryType) {
        if (summaryType == null || summaryType.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return Optional.empty();
        }

        Document document = docOpt.get();
    return documentSummaryRepository.findFirstByDocumentAndSummaryTypeIgnoreCaseOrderByCreatedAtDesc(document, summaryType.trim())
        .map(this::hydrateSummary);
    }

    @Override
    public List<DocumentSummary> getSummariesForDocument(String documentId) {
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return Collections.emptyList();
        }
        Document document = docOpt.get();
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

