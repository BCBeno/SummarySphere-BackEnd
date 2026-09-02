package com.beno.summaryspherebackend.events;

import com.beno.summaryspherebackend.services.AIService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SummaryRequestedListener {
    private final AIService aiService;

    public SummaryRequestedListener(AIService aiService) {
        this.aiService = aiService;
    }

    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSummaryRequested(SummaryRequestedEvent event) {
        aiService.processSummary(event.summaryId());
    }
}
