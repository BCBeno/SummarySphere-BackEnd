// src/main/java/com/beno/summaryspherebackend/controllers/DocumentController.java
package com.beno.summaryspherebackend.controllers;

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

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(path = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        try {
             documentService.storeFile(file);
            return ResponseEntity.ok("File uploaded successfully: " + originalFileName);
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

        Document doc = docOpt.get();

        boolean available;
        try {
            Resource res = documentService.loadFileAsResource(id);
            available = res.exists() && res.isReadable();
        } catch (IllegalArgumentException ex) {
            available = false;
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("id", doc.getId());
        meta.put("originalFileName", doc.getOriginalFilename());
        meta.put("size", doc.getSize());
        meta.put("status", available ? "AVAILABLE" : "MISSING");
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/documents/")
                .path(id)
                .path("/file")
                .toUriString();
        meta.put("downloadUrl", downloadUrl);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(meta);
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

