// src/main/java/com/beno/summaryspherebackend/controllers/DocumentController.java
package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.services.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final ConvertToDto convertToDto;

    public DocumentController(DocumentService documentService, ConvertToDto convertToDto) {
        this.documentService = documentService;
        this.convertToDto = convertToDto;
    }

    @PostMapping(path = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        try {
             String id = documentService.storeFile(file);
             HashMap<String, String> message = new HashMap<>();
             message.put("message", "Document uploaded successfully");
             message.put("id", id);
            return ResponseEntity.ok(message.toString());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("There was an error uploading the file: " + ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body("Could not store file " + originalFileName + ". Please try again!");
        }
    }

    @GetMapping("")
    public ResponseEntity<?> listFiles() {
        return ResponseEntity.ok(documentService.listFiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentMetadata(@PathVariable String id) {
        Optional<Document> docOpt = documentService.getDocumentById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertToDto.convertDocumentToDto(docOpt.get()));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable String id) {
        try {
            documentService.deleteFile(id);
            return ResponseEntity.ok("File deleted successfully: " + id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("There was an error deleting the file: " + ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body("Could not delete file " + id + ". Please try again!");
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> downloadFile(@PathVariable String id) {
        try {
            Resource resource = documentService.loadFileAsResource(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("There was an error downloading the file: " + ex.getMessage());
        }
    }

}

