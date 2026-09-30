package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.repositories.DocumentSummaryRepository;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.DocumentVectorService;
import com.beno.summaryspherebackend.services.FileExtractionService;
import com.beno.summaryspherebackend.services.ObjectStorageService;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt");
    private static final Map<String, String> MEDIA_TYPE_BY_EXTENSION = Map.of(
            ".pdf", "application/pdf",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".txt", "text/plain"
    );
    private final Tika tika = new Tika();
    private final DocumentRepository documentRepository;
    private final ConvertToDto convertToDto;
    private final FileExtractionService fileExtractionService;
    private final ObjectStorageService objectStorageService;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final DocumentVectorService documentVectorService;

    public DocumentServiceImpl(DocumentRepository documentRepository, ConvertToDto convertToDto,
            FileExtractionService fileExtractionService, ObjectStorageService objectStorageService,
            DocumentSummaryRepository documentSummaryRepository,
            DocumentVectorService documentVectorService) {
        this.fileExtractionService = fileExtractionService;
        this.documentRepository = documentRepository;
        this.convertToDto = convertToDto;
        this.objectStorageService = objectStorageService;
        this.documentSummaryRepository = documentSummaryRepository;
        this.documentVectorService = documentVectorService;
    }

    @Override
    public String storeFile(MultipartFile file, String title, User uploader) throws IOException {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank() || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty file with a filename is required");
        }
        String docTitle = (title != null && !title.trim().isEmpty()) ? title : originalFileName;
        long fileSize = file.getSize();

        if (fileSize > 20L * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 20MB");
        }

        int dotIndex = originalFileName.lastIndexOf('.');
        String fileExtension;
        if (dotIndex >= 0 && dotIndex < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(dotIndex).toLowerCase(Locale.ROOT);
        } else {
            throw new IllegalArgumentException("File must have an extension");
        }

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: txt, pdf, docx");
        }

        String expectedMediaType = MEDIA_TYPE_BY_EXTENSION.get(fileExtension);
        String detectedType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedType = tika.detect(inputStream, originalFileName);
            if (detectedType == null || !expectedMediaType.equalsIgnoreCase(detectedType)) {
                throw new IllegalArgumentException("Unsupported or invalid file content");
            }
        }

        String uniqueFileName = UUID.randomUUID() + fileExtension;

        String content;
        try (InputStream extractionStream = file.getInputStream()) {
            content = fileExtractionService.extractText(extractionStream);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "File could not be processed. Check that it is a valid PDF, DOCX, or TXT file.");
        }

        try (InputStream uploadStream = file.getInputStream()) {
            objectStorageService.upload(uniqueFileName, uploadStream, fileSize, expectedMediaType);
        }

        String contentBlobName = buildContentBlobName(uniqueFileName);
        uploadTextBlob(contentBlobName, content);

        Document document = new Document(uniqueFileName, docTitle, originalFileName, fileSize, fileExtension,
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
    public Document getOwnedDocument(String id, String userId) {
        return hydrateDocumentContent(requireOwnedDocument(id, userId));
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
    public void deleteOwnedDocument(String id, String userId) {
        Document document = requireOwnedDocument(id, userId);

        // delete vector store chunks
        try {
            documentVectorService.deleteDocumentChunks(id);
        } catch (Exception e) {
            log.warn("Failed to delete vector chunks for document {}", id, e);
        }

        // delete original file object
        objectStorageService.delete(id);

        // delete extracted text object
        if (document.getContentBlobName() != null && !document.getContentBlobName().isBlank()) {
            objectStorageService.delete(document.getContentBlobName());
        }

        // delete generated summary objects
        try {
            var summaries = documentSummaryRepository.findAllByDocument(document);
            for (var summary : summaries) {
                if (summary.getSummaryBlobName() != null && !summary.getSummaryBlobName().isBlank()) {
                    objectStorageService.delete(summary.getSummaryBlobName());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to delete summary objects for document {}", id, ex);
        }

        // Delete the document. CascadeType.ALL on 'summaries' and 'chatMessages'
        // will automatically delete associated database rows.
        documentRepository.delete(document);
    }

    @Override
    public String createOwnedDownloadUrl(String id, String userId) {
        requireOwnedDocument(id, userId);
        if (!objectStorageService.exists(id)) {
            throw new IllegalArgumentException("File not found with id" + id);
        }
        return objectStorageService.createPresignedDownloadUrl(id, Duration.ofMinutes(5));
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

            objectStorageService.delete(doc.getDocumentId());
            if (doc.getContentBlobName() != null && !doc.getContentBlobName().isBlank()) {
                objectStorageService.delete(doc.getContentBlobName());
            }

            try {
                var summaries = documentSummaryRepository.findAllByDocument(doc);
                for (var summary : summaries) {
                    if (summary.getSummaryBlobName() != null && !summary.getSummaryBlobName().isBlank()) {
                        objectStorageService.delete(summary.getSummaryBlobName());
                    }
                }
            } catch (Exception ex) {
                // ignore and continue with next document
            }
        }
        documentRepository.deleteAll(userFileList);
    }

    private Document requireOwnedDocument(String id, String userId) {
        if (userId == null || id == null) {
            throw new jakarta.persistence.EntityNotFoundException("Document not found.");
        }
        return documentRepository.findByDocumentIdAndUploadedById(id, userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Document not found."));
    }

    private String buildContentBlobName(String documentId) {
        return "documents/" + documentId + "/content.txt";
    }

    private void uploadTextBlob(String blobName, String content) throws IOException {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(contentBytes)) {
            objectStorageService.upload(blobName, dataStream, contentBytes.length, "text/plain; charset=utf-8");
        }
    }

    private Document hydrateDocumentContent(Document document) {
        if (document == null) {
            return null;
        }

        if (document.getContentBlobName() != null && !document.getContentBlobName().isBlank()) {
            if (!objectStorageService.exists(document.getContentBlobName())) {
                return document;
            }
            document.setContent(new String(objectStorageService.download(document.getContentBlobName()),
                    StandardCharsets.UTF_8));
        }

        return document;
    }
}
