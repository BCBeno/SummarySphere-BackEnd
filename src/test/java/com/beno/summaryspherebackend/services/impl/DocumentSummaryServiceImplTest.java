package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentSummaryServiceImplTest {
    @Mock
    DocumentSummaryRepository documentSummaryRepository;
    @Mock
    DocumentRepository documentRepository;

    @InjectMocks
    DocumentSummaryServiceImpl documentSummaryService;

    @Test
    void getLatestSummaryForDocument_returnsEmpty_whenDocumentMissing() {
        when(documentRepository.findById("missing")).thenReturn(Optional.empty());

        assertTrue(documentSummaryService.getLatestSummaryForDocument("missing").isEmpty());
        verify(documentSummaryRepository, never()).findFirstByDocumentOrderByCreatedAtDesc(any());
    }

    @Test
    void getLatestSummaryForDocument_returnsLatest_whenDocumentExists() {
        Document doc = new Document("id", "t", "o.pdf", 1L, ".pdf", "content", null);
        DocumentSummary summary = new DocumentSummary();

        when(documentRepository.findById("id")).thenReturn(Optional.of(doc));
        when(documentSummaryRepository.findFirstByDocumentOrderByCreatedAtDesc(doc)).thenReturn(Optional.of(summary));

        assertEquals(Optional.of(summary), documentSummaryService.getLatestSummaryForDocument("id"));
    }

    @Test
    void getLatestSummaryForDocumentByType_returnsEmpty_whenTypeBlank() {
        assertTrue(documentSummaryService.getLatestSummaryForDocumentByType("id", " ").isEmpty());
        verifyNoInteractions(documentRepository);
        verifyNoInteractions(documentSummaryRepository);
    }

    @Test
    void getLatestSummaryForDocumentByType_returnsEmpty_whenDocumentMissing() {
        when(documentRepository.findById("missing")).thenReturn(Optional.empty());

        assertTrue(documentSummaryService.getLatestSummaryForDocumentByType("missing", "short").isEmpty());
        verify(documentSummaryRepository, never()).findFirstByDocumentAndSummaryTypeIgnoreCaseOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getLatestSummaryForDocumentByType_callsRepositoryWithTrimmedType() {
        Document doc = new Document("id", "t", "o.pdf", 1L, ".pdf", "content", null);
        DocumentSummary summary = new DocumentSummary();

        when(documentRepository.findById("id")).thenReturn(Optional.of(doc));
        when(documentSummaryRepository.findFirstByDocumentAndSummaryTypeIgnoreCaseOrderByCreatedAtDesc(doc, "short"))
                .thenReturn(Optional.of(summary));

        assertEquals(Optional.of(summary), documentSummaryService.getLatestSummaryForDocumentByType("id", " short "));
    }

    @Test
    void getSummariesForDocument_returnsEmptyList_whenDocumentMissing() {
        when(documentRepository.findById("missing")).thenReturn(Optional.empty());

        List<DocumentSummary> result = documentSummaryService.getSummariesForDocument("missing");
        assertEquals(Collections.emptyList(), result);
        verify(documentSummaryRepository, never()).findAllByDocumentOrderByCreatedAtDesc(any());
    }
}