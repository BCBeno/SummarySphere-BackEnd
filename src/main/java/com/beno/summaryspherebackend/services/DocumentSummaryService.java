package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.entities.User;

import java.util.List;
import java.util.Optional;

public interface DocumentSummaryService {
    DocumentSummary requestSummary(String documentId, String summaryType, User currentUser);

    Optional<DocumentSummary> getLatestSummaryForDocument(String documentId);

    Optional<DocumentSummary> getLatestSummaryForDocumentByType(String documentId, String summaryType);

    List<DocumentSummary> getSummariesForDocument(String documentId);
}

