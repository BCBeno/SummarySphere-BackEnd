package com.beno.summaryspherebackend.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.DocumentVectorService;
import com.beno.summaryspherebackend.services.FileExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt");
    private final DocumentRepository documentRepository;
    private final ConvertToDto convertToDto;
    private final FileExtractionService fileExtractionService;
    private final BlobContainerClient blobContainerClient;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentVectorService documentVectorService;

    public DocumentServiceImpl(DocumentRepository documentRepository, ConvertToDto convertToDto,
            FileExtractionService fileExtractionService, BlobContainerClient blobContainerClient,
            DocumentSummaryRepository documentSummaryRepository,
            DocumentVectorService documentVectorService) {
        this.fileExtractionService = fileExtractionService;
        this.documentRepository = documentRepository;
        this.convertToDto = convertToDto;
        this.blobContainerClient = blobContainerClient;
        this.documentSummaryRepository = documentSummaryRepository;
        this.documentVectorService = documentVectorService;
    }

    @Override
    public String storeFile(MultipartFile file, String title, User uploader) throws IOException {
        byte[] bytes = file.getBytes(); // COST MARE DE MEMORIE. De rezolvat în viitor.
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String docTitle = (title != null && !title.trim().isEmpty()) ? title : originalFileName;

        if (file.getSize() > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 25MB");
        }

        int dotIndex = originalFileName.lastIndexOf('.');
        String fileExtension;
        if (dotIndex >= 0 && dotIndex < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(dotIndex).toLowerCase();
        } else {
            throw new IllegalArgumentException("File must have an extension");
        }

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: txt, pdf, docx");
        }

        String uniqueFileName = UUID.randomUUID() + fileExtension;

        String content;
        try {
            content = fileExtractionService.extractTextFromBytes(bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Extraction failed: " + e.getMessage());
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(uniqueFileName);
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(bytes)) {
            blobClient.upload(dataStream, bytes.length, true);
        }

        String contentBlobName = buildContentBlobName(uniqueFileName);
        uploadTextBlob(contentBlobName, content);

        Document document = new Document(uniqueFileName, docTitle, originalFileName, (long) bytes.length, fileExtension,
                null, uploader);
        document.setContentBlobName(contentBlobName);
        documentRepository.save(document);

        // Chunk and embed the document content for RAG-based chat
        try {
            documentVectorService.ingestDocument(uniqueFileName, content);
        } catch (Exception e) {
            log.warn("Failed to generate vector embeddings for document {}. Chat will use full content fallback.",
                    uniqueFileName, e);
        }

        return uniqueFileName;
    }

    @Override
    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id)
                .map(this::hydrateDocumentContent);
    }

    @Override
    public List<DocumentListDTO> listFiles() {
        return documentRepository.findAll().stream()
                .map(convertToDto::convertDocumentListToDto)
                .toList();
    }

    @Override
    public List<DocumentListDTO> listFilesByUser(User user) {
        return documentRepository.findByUploadedBy(user).stream()
                .map(convertToDto::convertDocumentListToDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteFile(String id) {
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("File not found with id " + id);
        }

        Document document = documentOpt.get();

        // delete vector store chunks
        try {
            documentVectorService.deleteDocumentChunks(id);
        } catch (Exception e) {
            log.warn("Failed to delete vector chunks for document {}", id, e);
        }

        // delete main blob
        BlobClient blobClient = blobContainerClient.getBlobClient(id);
        blobClient.deleteIfExists();

        // delete extracted content blob
        if (document.getContentBlobName() != null && !document.getContentBlobName().isBlank()) {
            blobContainerClient.getBlobClient(document.getContentBlobName()).deleteIfExists();
        }

        // delete any summary blobs from Azure Storage
        try {
            var summaries = documentSummaryRepository.findAllByDocument(document);
            for (var summary : summaries) {
                if (summary.getSummaryBlobName() != null && !summary.getSummaryBlobName().isBlank()) {
                    blobContainerClient.getBlobClient(summary.getSummaryBlobName()).deleteIfExists();
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to delete summary blobs for document {}", id, ex);
        }

        // Delete the document. CascadeType.ALL on 'summaries' and 'chatMessages'
        // will automatically delete associated database rows.
        documentRepository.delete(document);
    }

    @Override
    public String generateDownloadLink(String id) {
        BlobClient blobClient = blobContainerClient.getBlobClient(id);

        if (!blobClient.exists()) {
            throw new IllegalArgumentException("File not found with id" + id);
        }

        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(10);

        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permissions)
                .setStartTime(OffsetDateTime.now().minusMinutes(1));

        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    @Override
    @Transactional
    public void deleteFilesByUser(User user) {
        List<Document> userFileList = documentRepository.findByUploadedBy(user);
        for (Document doc : userFileList) {
            // delete vector store chunks
            try {
                documentVectorService.deleteDocumentChunks(doc.getDocumentId());
            } catch (Exception e) {
                log.warn("Failed to delete vector chunks for document {}", doc.getDocumentId(), e);
            }

            BlobClient blobClient = blobContainerClient.getBlobClient(doc.getDocumentId());
            blobClient.deleteIfExists();
            if (doc.getContentBlobName() != null && !doc.getContentBlobName().isBlank()) {
                blobContainerClient.getBlobClient(doc.getContentBlobName()).deleteIfExists();
            }

            try {
                var summaries = documentSummaryRepository.findAllByDocument(doc);
                for (var summary : summaries) {
                    if (summary.getSummaryBlobName() != null && !summary.getSummaryBlobName().isBlank()) {
                        blobContainerClient.getBlobClient(summary.getSummaryBlobName()).deleteIfExists();
                    }
                }
            } catch (Exception ex) {
                // ignore and continue with next document
            }
        }
        documentRepository.deleteAll(userFileList);
    }

    private String buildContentBlobName(String documentId) {
        return "documents/" + documentId + "/content.txt";
    }

    private void uploadTextBlob(String blobName, String content) throws IOException {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(contentBytes)) {
            blobClient.upload(dataStream, contentBytes.length, true);
        }
    }

    private Document hydrateDocumentContent(Document document) {
        if (document == null) {
            return null;
        }

        if (document.getContentBlobName() != null && !document.getContentBlobName().isBlank()) {
            BlobClient blobClient = blobContainerClient.getBlobClient(document.getContentBlobName());
            if (!blobClient.exists()) {
                return document;
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                blobClient.downloadStream(outputStream);
                document.setContent(outputStream.toString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the document content from blob storage.", e);
            }
        }

        return document;
    }
}