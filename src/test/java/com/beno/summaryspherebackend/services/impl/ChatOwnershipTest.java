package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.ChatMessageRepository;
import com.beno.summaryspherebackend.services.DocumentVectorService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ChatOwnershipTest {
    @Test
    void inaccessibleChatNeverReadsHistoryOrCallsVectorsOrAi() {
        DocumentRepository documents = mock(DocumentRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        DocumentVectorService vectors = mock(DocumentVectorService.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        when(builder.build()).thenReturn(client);
        ChatPersistenceService persistence = new ChatPersistenceService(documents, messages);
        ChatServiceImpl service = new ChatServiceImpl(builder, messages, documents, vectors, persistence);
        User user = User.builder().id("user").build();

        assertThrows(EntityNotFoundException.class, () -> service.sendMessage("other.pdf", "hello", user));
        assertThrows(EntityNotFoundException.class, () -> service.getChatHistory("other.pdf", user));
        assertThrows(EntityNotFoundException.class, () -> service.clearChatHistory("other.pdf", user));
        verify(documents, times(3)).findByDocumentIdAndUploadedById("other.pdf", "user");
        verifyNoInteractions(messages, vectors, client);
    }
}
