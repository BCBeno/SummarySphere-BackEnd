package com.beno.summaryspherebackend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.beno.summaryspherebackend.services.JwtService;
import org.springframework.mail.javamail.JavaMailSender;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobContainerClient;
import org.springframework.ai.chat.client.ChatClient;
import com.beno.summaryspherebackend.services.DocumentVectorService;
import org.springframework.ai.vectorstore.VectorStore;

import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public JwtService testJwtService() {
        // Provide a Mockito mock JwtService for tests so that context startup doesn't require full JWT wiring
        return mock(JwtService.class);
    }

    @Bean
    @Primary
    public JavaMailSender testMailSender() {
        // Provide a Mockito mock JavaMailSender so EmailService and beans depending on it can initialize in tests
        return mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public BlobServiceClient testBlobServiceClient() {
        // Mock the Azure BlobServiceClient used by AzureBlobConfig so tests don't need a real connection string
        BlobServiceClient client = mock(BlobServiceClient.class);
        BlobContainerClient container = mock(BlobContainerClient.class);
        // ensure getBlobContainerClient returns our mock container and exists() returns true to skip creation
        when(client.getBlobContainerClient(ArgumentMatchers.anyString())).thenReturn(container);
        when(container.exists()).thenReturn(true);
        return client;
    }

    @Bean
    @Primary
    public BlobContainerClient testBlobContainerClient() {
        // Provide the same mocked container client used by the BlobServiceClient
        BlobContainerClient container = mock(BlobContainerClient.class);
        when(container.exists()).thenReturn(true);
        return container;
    }

    @Bean
    @Primary
    public ChatClient.Builder testChatClientBuilder() {
        // Mock the ChatClient.Builder so AIServiceImpl can be constructed during tests without the real client
        return mock(ChatClient.Builder.class);
    }

    @Bean
    @Primary
    public ChatClient testChatClient() {
        // Mock ChatClient as well in case any code autowires it directly
        return mock(ChatClient.class);
    }

    @Bean
    @Primary
    public DocumentVectorService testDocumentVectorService() {
        // Provide a mock DocumentVectorService so test context starts without a real vector store
        return mock(DocumentVectorService.class);
    }

    @Bean
    @Primary
    public VectorStore testVectorStore() {
        // Provide a mock VectorStore so the context can start without pgvector
        return mock(VectorStore.class);
    }
}
