package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.events.SummaryRequestedEvent;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.RateLimitService;
import org.springframework.context.ApplicationEventPublisher;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentSummaryServiceImplTest {
    @Mock
    DocumentSummaryRepository documentSummaryRepository;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    BlobContainerClient blobContainerClient;
    @Mock
    RateLimitService rateLimitService;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    DocumentSummaryServiceImpl documentSummaryService;

    @Test
    void requestSummary_savesPendingBeforePublishingEvent() {
        User user = new User();
        user.setId("user-1");
        Document document = new Document("doc-1", "title", "file.pdf", 1L, ".pdf", null, user);
        when(documentRepository.findByIdForUpdate("doc-1")).thenReturn(Optional.of(document));
        when(documentSummaryRepository.existsByDocumentAndSummaryTypeIgnoreCaseAndStatusIn(
                eq(document), eq("concise"), anyList())).thenReturn(false);
        when(documentSummaryRepository.save(any(DocumentSummary.class))).thenAnswer(invocation -> {
            DocumentSummary summary = invocation.getArgument(0);
            summary.setId(42L);
            return summary;
        });

        DocumentSummary result = documentSummaryService.requestSummary("doc-1", " concise ", user);

        assertEquals(42L, result.getId());
        assertEquals("concise", result.getSummaryType());
        assertEquals(SummaryStatus.PENDING, result.getStatus());
        assertEquals(0, result.getAttemptCount());
        assertEquals(SummaryStatus.PENDING.name(), document.getStatus());
        verify(rateLimitService).checkSummarization("user-1");
        verify(eventPublisher).publishEvent(new SummaryRequestedEvent(42L));
    }

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
