package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.DocumentSummary;

import java.util.Optional;

public interface DocumentSummaryService {
    Optional<DocumentSummary> getLatestSummaryForDocument(String documentId);
}

