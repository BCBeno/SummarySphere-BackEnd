package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.*;
import com.beno.summaryspherebackend.services.agent.AgentTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AgentToolsOwnershipTest {
    @Test
    void guessedIdsNeverReadBlobOrTriggerSummaries() {
        DocumentRepository documents = mock(DocumentRepository.class);
        BlobContainerClient blobs = mock(BlobContainerClient.class);
        DocumentVectorService vectors = mock(DocumentVectorService.class);
        DocumentSummaryRepository summaryRepository = mock(DocumentSummaryRepository.class);
        DocumentSummaryService summaries = mock(DocumentSummaryService.class);
        DocumentService service = new DocumentServiceImpl(documents, mock(ConvertToDto.class),
                mock(FileExtractionService.class), blobs, summaryRepository, vectors);
        AgentTools tools = new AgentTools(service, summaries, documents);
        ToolContext context = new ToolContext(Map.of("currentUser", User.builder().id("user").build()));

        assertEquals("Document not found with ID: other.pdf", tools.getDocumentContent("other.pdf", context));
        assertEquals("Document not found with ID: other.pdf", tools.getDocumentSummary("other.pdf", "concise", context));
        assertEquals("Document not found with ID: other.pdf", tools.triggerSummarization("other.pdf", "concise", context));
        assertEquals("Document not found with ID: other.pdf", tools.generateQuiz("other.pdf", 3, context));
        verify(documents, times(4)).findByDocumentIdAndUploadedById("other.pdf", "user");
        verifyNoInteractions(blobs, vectors, summaryRepository, summaries);
    }
}
