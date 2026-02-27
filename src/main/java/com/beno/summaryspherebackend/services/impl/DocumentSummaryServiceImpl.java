package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.DocumentSummaryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DocumentSummaryServiceImpl implements DocumentSummaryService {

    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentRepository documentRepository;

    public DocumentSummaryServiceImpl(DocumentSummaryRepository documentSummaryRepository, DocumentRepository documentRepository) {
        this.documentSummaryRepository = documentSummaryRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    public Optional<DocumentSummary> getLatestSummaryForDocument(String documentId) {
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return Optional.empty();
        }
        Document document = docOpt.get();
        return documentSummaryRepository.findFirstByDocumentOrderByCreatedAtDesc(document);
    }
}

