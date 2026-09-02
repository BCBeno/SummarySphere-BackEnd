package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.enums.SummaryStatus;
import com.beno.summaryspherebackend.events.SummaryRequestedEvent;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SummaryRecoveryJobTest {
    @Test
    void retryStuckSummaries_republishesEachStuckSummary() {
        DocumentSummaryRepository repository = mock(DocumentSummaryRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        when(repository.findStuckSummaryIds(
                eq(SummaryStatus.PENDING), eq(SummaryStatus.PROCESSING), any(LocalDateTime.class)))
                .thenReturn(List.of(10L, 11L));

        new SummaryRecoveryJob(repository, publisher).retryStuckSummaries();

        verify(publisher).publishEvent(new SummaryRequestedEvent(10L));
        verify(publisher).publishEvent(new SummaryRequestedEvent(11L));
    }
}
