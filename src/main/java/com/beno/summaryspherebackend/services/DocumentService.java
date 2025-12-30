package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.dtos.DocumentDTO;
import com.beno.summaryspherebackend.dtos.DocumentListDTO;
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
    ConvertToDto convertToDto;
    FileExtractionService fileExtractionService;

    public DocumentService(DocumentRepository documentRepository, ConvertToDto convertToDto, FileExtractionService fileExtractionService) {
        this.fileExtractionService = fileExtractionService;
        this.documentRepository = documentRepository;
        this.convertToDto = convertToDto;
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String title) throws IOException {
        byte[] bytes = file.getBytes();
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        
        String docTitle = (title != null && !title.trim().isEmpty()) ? title : originalFileName;

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

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence " + originalFileName);
        }

        String content;
        try {
            content = fileExtractionService.extractTextFromBytes(bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Extraction failed: " + e.getMessage());
        }

        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
        Files.write(targetLocation, bytes);
        
        documentRepository.save(new Document(uniqueFileName, docTitle, originalFileName, (long)bytes.length, fileExtension, content));
        return uniqueFileName;
    }

    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id);
    }

    public List<DocumentListDTO> listFiles() {
        List<Document> documents = documentRepository.findAll();
        List<DocumentListDTO> documentDTOs = new ArrayList<>();
        documents.forEach(doc -> documentDTOs.add(convertToDto.convertDocumentListToDto(doc)));
        return documentDTOs;
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