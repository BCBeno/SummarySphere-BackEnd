package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryProcessingStateServiceTest {
    @Mock
    DocumentSummaryRepository repository;

    @Test
    void claimForProcessing_movesPendingToProcessingAndIncrementsAttempts() {
        DocumentSummary summary = pendingSummary(1L, 0);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(summary));
        SummaryProcessingStateService service = new SummaryProcessingStateService(repository);

        var work = service.claimForProcessing(1L);

        assertTrue(work.isPresent());
        assertEquals("doc-1", work.get().documentId());
        assertEquals(SummaryStatus.PROCESSING, summary.getStatus());
        assertEquals(1, summary.getAttemptCount());
        assertEquals(SummaryStatus.PROCESSING.name(), summary.getDocument().getStatus());
    }

    @Test
    void claimForProcessing_marksSummaryFailedAfterMaximumAttempts() {
        DocumentSummary summary = pendingSummary(2L, SummaryProcessingStateService.MAX_ATTEMPTS);
        when(repository.findByIdForUpdate(2L)).thenReturn(Optional.of(summary));
        SummaryProcessingStateService service = new SummaryProcessingStateService(repository);

        assertTrue(service.claimForProcessing(2L).isEmpty());
        assertEquals(SummaryStatus.FAILED, summary.getStatus());
        assertEquals(SummaryStatus.FAILED.name(), summary.getDocument().getStatus());
    }

    @Test
    void claimForProcessing_reclaimsStaleProcessingSummary() {
        DocumentSummary summary = pendingSummary(3L, 1);
        summary.setStatus(SummaryStatus.PROCESSING);
        summary.setLastAttemptAt(LocalDateTime.now().minusMinutes(4));
        when(repository.findByIdForUpdate(3L)).thenReturn(Optional.of(summary));
        SummaryProcessingStateService service = new SummaryProcessingStateService(repository);

        assertTrue(service.claimForProcessing(3L).isPresent());
        assertEquals(2, summary.getAttemptCount());
    }

    @Test
    void markCompleted_persistsBlobAndCompletedStatus() {
        DocumentSummary summary = pendingSummary(4L, 1);
        summary.setStatus(SummaryStatus.PROCESSING);
        when(repository.findByIdForUpdate(4L)).thenReturn(Optional.of(summary));
        SummaryProcessingStateService service = new SummaryProcessingStateService(repository);

        service.markCompleted(4L, "summaries/doc-1/result.txt");

        assertEquals(SummaryStatus.COMPLETED, summary.getStatus());
        assertEquals("summaries/doc-1/result.txt", summary.getSummaryBlobName());
        assertEquals(SummaryStatus.COMPLETED.name(), summary.getDocument().getStatus());
    }

    private DocumentSummary pendingSummary(Long id, int attempts) {
        Document document = new Document();
        document.setDocumentId("doc-1");
        DocumentSummary summary = new DocumentSummary();
        summary.setId(id);
        summary.setDocument(document);
        summary.setSummaryType("concise");
        summary.setStatus(SummaryStatus.PENDING);
        summary.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        summary.setAttemptCount(attempts);
        return summary;
    }
}
