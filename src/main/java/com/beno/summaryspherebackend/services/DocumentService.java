package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class DocumentService {

    private final Path fileStorageLocation;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".txt");
    DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public void storeFile(MultipartFile file) throws IOException {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = "";
        if(file.getSize() > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 25MB");
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(dotIndex).toLowerCase();
        } else {
            throw new IllegalArgumentException("File must have an extension");
        }

        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: txt, pdf, docx");
        }
        String uniqueFileName = UUID.randomUUID() + fileExtension;

        if (uniqueFileName.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence " + uniqueFileName);
        }

        documentRepository.save(new Document(uniqueFileName, originalFileName, file.getSize()));
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
    }

    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id);
    }

    public List<Document> listFiles()
    {
        List<Document> documents = documentRepository.findAll();
        return documents;
    }

    public void deleteFile(String id) throws IOException {
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("File not found with id " + id);
        }
        Path filePath = this.fileStorageLocation.resolve(id).normalize();
        Files.deleteIfExists(filePath);
        documentRepository.deleteById(id);
    }

    public Resource loadFileAsResource(String id) {
        try {
            Path filePath = this.fileStorageLocation.resolve(id).normalize();
            UrlResource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found or not readable: " + id);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("File not found: " + id, ex);
        }
    }

}



