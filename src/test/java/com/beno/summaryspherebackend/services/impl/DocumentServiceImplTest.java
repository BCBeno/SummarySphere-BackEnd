package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.services.FileExtractionService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    DocumentRepository documentRepository;

    @Mock
    com.beno.summaryspherebackend.repositories.DocumentSummaryRepository documentSummaryRepository;

    @Mock
    BlobContainerClient blobContainerClient;

    @Mock
    ConvertToDto convertToDto;

    @Mock
    FileExtractionService fileExtractionService;

    @Mock
    com.beno.summaryspherebackend.services.DocumentVectorService documentVectorService;

    @InjectMocks
    DocumentServiceImpl documentService;


    @Test
    void storeFileInBlobStorage_success() throws Exception {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        byte[] contentBytes = "hello world".getBytes();
        when(file.getInputStream())
            .thenReturn(new ByteArrayInputStream(contentBytes), new ByteArrayInputStream(contentBytes));
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn((long) contentBytes.length);

        when(fileExtractionService.extractText(any(InputStream.class))).thenReturn("extracted text");

        BlobClient blobClient = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        // simulate successful upload
        doNothing().when(blobClient).upload(any(InputStream.class), anyLong(), anyBoolean());

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User uploader = User.builder()
                .email("a@b.com")
                .password("pass")
                .fullName("Test User")
                .role(Role.USER)
                .build();

        // Act
        String returnedId = documentService.storeFile(file, "", uploader);

        // Assert
        assertNotNull(returnedId);
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, times(1)).save(captor.capture());
        Document saved = captor.getValue();
        assertEquals("test.pdf", saved.getOriginalFilename());
        assertEquals((long) contentBytes.length, saved.getSize());
        assertEquals(uploader, saved.getUploadedBy());
        verify(blobContainerClient, times(2)).getBlobClient(anyString());
        verify(blobClient, times(2)).upload(any(InputStream.class), anyLong(), anyBoolean());
    }

    @Test
    void store_FileTooLargeThrows() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("largefile.pdf");
        when(file.getSize()).thenReturn(26L * 1024 * 1024); // 26MB
        User uploader = User.builder()
                .email("a@b.com")
                .password("pass")
                .fullName("Test User")
                .role(Role.USER)
                .build();
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> documentService.storeFile(file, "title", uploader));

    }

    @Test
    void store_FileWithoutExtensionThrows() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("filewithoutextension");
        when(file.getSize()).thenReturn(1024L); // 1KB
        User uploader = User.builder()
                .email("a@b.com")
                .password("pass")
                .fullName("Test User")
                .role(Role.USER)
                .build();


        assertThrows(IllegalArgumentException.class, () -> documentService.storeFile(file, "title", uploader));
    }

    @Test
    void generateDownloadLink_whenBlobExists_returnsLink() {
        // Arrange
        String id = "file-id.pdf";
        when(documentRepository.findByDocumentIdAndUploadedById(id, "user")).thenReturn(Optional.of(new Document()));
        BlobClient blobClient = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient(id)).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.getBlobUrl()).thenReturn("http://storage.example.com/container/" + id);
        when(blobClient.generateSas(any())).thenReturn("sastoken123");

        // Act
        String link = documentService.createOwnedDownloadUrl(id, "user");

        // Assert
        assertEquals("http://storage.example.com/container/" + id + "?sastoken123", link);
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(1)).generateSas(any());
    }

    @Test
    void generateDownloadLink_whenBlobMissing_throws() {
        // Arrange
        String id = "missing-file.pdf";
        when(documentRepository.findByDocumentIdAndUploadedById(id, "user")).thenReturn(Optional.of(new Document()));
        BlobClient blobClient = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient(id)).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> documentService.createOwnedDownloadUrl(id, "user"));
        verify(blobClient, times(1)).exists();
        verify(blobClient, times(0)).generateSas(any());
    }

    @Test
    void deleteFile_whenNotFound_throws() {
        // Arrange
        String id = "missing-id";
        when(documentRepository.findByDocumentIdAndUploadedById(id, "user")).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> documentService.deleteOwnedDocument(id, "user"));
    }

    @Test
    void deleteFile_whenFound_deletesBlobAndRepository() {
        // Arrange
        String id = "present-id";
        Document doc = new Document(id, "title", "orig.pdf", 123L, ".pdf", "content", null);
        when(documentRepository.findByDocumentIdAndUploadedById(id, "user")).thenReturn(Optional.of(doc));
        when(documentSummaryRepository.findAllByDocument(doc)).thenReturn(java.util.Collections.emptyList());
        BlobClient blobClient = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient(id)).thenReturn(blobClient);
        when(blobClient.deleteIfExists()).thenReturn(true);

        // Act
        documentService.deleteOwnedDocument(id, "user");

        // Assert
        verify(blobClient, times(1)).deleteIfExists();
        verify(documentRepository, times(1)).delete(doc);
    }
    @Test
    void inaccessibleDocumentNeverTouchesStorageOrVectors() {
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> documentService.getOwnedDocument("other.pdf", "user"));
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> documentService.createOwnedDownloadUrl("other.pdf", "user"));
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> documentService.deleteOwnedDocument("other.pdf", "user"));
        verifyNoInteractions(blobContainerClient, documentVectorService, documentSummaryRepository);
        verify(documentRepository, never()).findById(anyString());
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void ownedContentIsHydratedAfterOwnerQuery() {
        Document doc = new Document();
        doc.setContentBlobName("content.txt");
        BlobClient blob = mock(BlobClient.class);
        when(documentRepository.findByDocumentIdAndUploadedById("doc.pdf", "user")).thenReturn(Optional.of(doc));
        when(blobContainerClient.getBlobClient("content.txt")).thenReturn(blob);
        when(blob.exists()).thenReturn(true);
        doAnswer(invocation -> {
            ((java.io.OutputStream) invocation.getArgument(0)).write("content".getBytes());
            return null;
        }).when(blob).downloadStream(any(java.io.OutputStream.class));
        assertEquals("content", documentService.getOwnedDocument("doc.pdf", "user").getContent());
        var order = inOrder(documentRepository, blobContainerClient, blob);
        order.verify(documentRepository).findByDocumentIdAndUploadedById("doc.pdf", "user");
        order.verify(blobContainerClient).getBlobClient("content.txt");
        order.verify(blob).exists();
        order.verify(blob).downloadStream(any(java.io.OutputStream.class));
    }}