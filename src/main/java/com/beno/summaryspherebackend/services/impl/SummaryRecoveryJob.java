package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.events.SummaryRequestedEvent;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class SummaryRecoveryJob {
    private final DocumentSummaryRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public SummaryRecoveryJob(DocumentSummaryRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${summary.recovery.fixed-delay-ms:60000}")
    @Transactional
    public void retryStuckSummaries() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(SummaryProcessingStateService.STUCK_AFTER_MINUTES);
        repository.findStuckSummaryIds(SummaryStatus.PENDING, SummaryStatus.PROCESSING, cutoff)
                .forEach(summaryId -> eventPublisher.publishEvent(new SummaryRequestedEvent(summaryId)));
    }
}
