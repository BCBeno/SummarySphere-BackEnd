package com.beno.summaryspherebackend.controllers;

import com.beno.summaryspherebackend.ModelMappers.ConvertToDto;
import com.beno.summaryspherebackend.dtos.SummarizationSchema;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.DocumentSummaryService;
import com.beno.summaryspherebackend.services.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final ConvertToDto convertToDto;
    private SummarizationSchema summarizationRecord;
    private GeminiService geminiService;
    private final DocumentSummaryService documentSummaryService;

    public DocumentController(DocumentService documentService, ConvertToDto convertToDto, GeminiService geminiService, DocumentSummaryService documentSummaryService) {
        this.geminiService = geminiService;
        this.documentService = documentService;
        this.convertToDto = convertToDto;
        this.documentSummaryService = documentSummaryService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping(path = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        try {
             String id = documentService.storeFile(file, title);
             
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

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("")
    public ResponseEntity<?> listFiles() {
        return ResponseEntity.ok(documentService.listFiles());
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentMetadata(@PathVariable String id) {
        Optional<Document> docOpt = documentService.getDocumentById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertToDto.convertDocumentToDto(docOpt.get()));
    }


    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/{id}/summarize")
    public ResponseEntity<?> summarizeDocument(@PathVariable String id, @RequestBody SummarizationSchema.SummarizeRequest summarizeRequest) {
        try {
            String summary = geminiService.summarizeAsync(id, summarizeRequest.summaryType());
            SummarizationSchema.SummarizeResponse response = new SummarizationSchema.SummarizeResponse(summary, id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("There was an error summarizing the document: " + ex.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}/summary")
    public ResponseEntity<?> getLatestSummary(@PathVariable String id) {
        Optional<DocumentSummary> summaryOpt = documentSummaryService.getLatestSummaryForDocument(id);
        if (summaryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DocumentSummary summary = summaryOpt.get();
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("documentId", id);
        resp.put("summaryType", summary.getSummaryType());
        resp.put("summaryText", summary.getSummaryText());
        resp.put("status", summary.getStatus());
        resp.put("createdAt", summary.getCreatedAt());
        return ResponseEntity.ok(resp);
    }
}