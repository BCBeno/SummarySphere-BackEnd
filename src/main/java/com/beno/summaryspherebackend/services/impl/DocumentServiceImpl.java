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
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.FileExtractionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt");
    private final DocumentRepository documentRepository;
    private final ConvertToDto convertToDto;
    private final FileExtractionService fileExtractionService;
    private final BlobContainerClient blobContainerClient;

    public DocumentServiceImpl(DocumentRepository documentRepository, ConvertToDto convertToDto,
                               FileExtractionService fileExtractionService, BlobContainerClient blobContainerClient) {
        this.fileExtractionService = fileExtractionService;
        this.documentRepository = documentRepository;
        this.convertToDto = convertToDto;
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public String storeFile(MultipartFile file, String title, User uploader) throws IOException {
        byte[] bytes = file.getBytes(); // COST MARE DE MEMORIE. De rezolvat în viitor.
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String docTitle = (title != null && !title.trim().isEmpty()) ? title : originalFileName;

        if(file.getSize() > 25 * 1024 * 1024) {
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

        documentRepository.save(new Document(uniqueFileName, docTitle, originalFileName, (long)bytes.length, fileExtension, content, uploader));
        return uniqueFileName;
    }

    @Override
    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id);
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
    public void deleteFile(String id) {
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("File not found with id " + id);
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(id);
        blobClient.deleteIfExists();

        documentRepository.deleteById(id);
    }

    @Override
    public String generateDownloadLink(String id) {
        BlobClient blobClient = blobContainerClient.getBlobClient(id);

        if (!blobClient.exists()) {
            throw new IllegalArgumentException("Fișierul nu există în stocare: " + id);
        }

        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(10);

        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permissions)
                .setStartTime(OffsetDateTime.now().minusMinutes(1));

        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }


    @Override
    public void deleteFilesByUser(User user) {
        List<Document> userFileList = documentRepository.findByUploadedBy(user);
        for (Document doc : userFileList) {
            BlobClient blobClient = blobContainerClient.getBlobClient(doc.getDocumentId());
            blobClient.deleteIfExists();
        }
        documentRepository.deleteAll(userFileList);
    }
}