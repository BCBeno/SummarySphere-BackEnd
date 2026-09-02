package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SummaryProcessingStateService {
    static final int MAX_ATTEMPTS = 3;
    static final int STUCK_AFTER_MINUTES = 3;

    private final DocumentSummaryRepository repository;

    public SummaryProcessingStateService(DocumentSummaryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<SummaryWorkItem> claimForProcessing(Long summaryId) {
        Optional<DocumentSummary> summaryOptional = repository.findByIdForUpdate(summaryId);
        if (summaryOptional.isEmpty()) {
            return Optional.empty();
        }

        DocumentSummary summary = summaryOptional.get();
        LocalDateTime now = LocalDateTime.now();
        boolean pending = summary.getStatus() == SummaryStatus.PENDING;
        boolean staleProcessing = summary.getStatus() == SummaryStatus.PROCESSING
                && summary.getLastAttemptAt() != null
                && summary.getLastAttemptAt().isBefore(now.minusMinutes(STUCK_AFTER_MINUTES));
        if (!pending && !staleProcessing) {
            return Optional.empty();
        }

        if (summary.getAttemptCount() >= MAX_ATTEMPTS) {
            summary.setStatus(SummaryStatus.FAILED);
            summary.getDocument().setStatus(SummaryStatus.FAILED.name());
            return Optional.empty();
        }

        summary.setStatus(SummaryStatus.PROCESSING);
        summary.setAttemptCount(summary.getAttemptCount() + 1);
        summary.setLastAttemptAt(now);
        summary.getDocument().setStatus(SummaryStatus.PROCESSING.name());
        return Optional.of(new SummaryWorkItem(
                summary.getId(),
                summary.getDocument().getDocumentId(),
                summary.getSummaryType()));
    }

    @Transactional
    public void markCompleted(Long summaryId, String blobName) {
        repository.findByIdForUpdate(summaryId).ifPresent(summary -> {
            if (summary.getStatus() == SummaryStatus.PROCESSING) {
                summary.setSummaryBlobName(blobName);
                summary.setStatus(SummaryStatus.COMPLETED);
                summary.getDocument().setStatus(SummaryStatus.COMPLETED.name());
            }
        });
    }

    @Transactional
    public void markFailed(Long summaryId) {
        repository.findByIdForUpdate(summaryId).ifPresent(summary -> {
            if (summary.getStatus() == SummaryStatus.PROCESSING) {
                summary.setStatus(SummaryStatus.FAILED);
                summary.getDocument().setStatus(SummaryStatus.FAILED.name());
            }
        });
    }

    public record SummaryWorkItem(Long summaryId, String documentId, String summaryType) {
    }
}
