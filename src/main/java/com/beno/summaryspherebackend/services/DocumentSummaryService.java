package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.DocumentSummary;

import java.util.List;
import java.util.Optional;

public interface DocumentSummaryService {
    Optional<DocumentSummary> getLatestSummaryForDocument(String documentId);

    Optional<DocumentSummary> getLatestSummaryForDocumentByType(String documentId, String summaryType);

    List<DocumentSummary> getSummariesForDocument(String documentId);
}

