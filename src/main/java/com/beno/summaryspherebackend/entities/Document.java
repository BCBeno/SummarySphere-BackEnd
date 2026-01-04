package com.beno.summaryspherebackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {
    @Id
    private String documentId;
    private String title;
    private String originalFilename;
    private Long size;
    private String fileType;
    private String status;
    private LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentSummary> summaries = new ArrayList<>();

    public void addSummary(DocumentSummary summary) {
        summaries.add(summary);
        summary.setDocument(this);
    }

    public void removeSummary(DocumentSummary summary) {
        summaries.remove(summary);
        summary.setDocument(null);
    }

    @Column(columnDefinition = "TEXT")
    private String content;
    //private String uploadedBy;

    public Document(String documentId, String title, String originalFilename, Long size, String fileType, String content) {
        this.documentId = documentId;
        this.title = title;
        this.originalFilename = originalFilename;
        this.size = size;
        this.uploadedAt = LocalDateTime.now();
        this.fileType = fileType;
        this.content = content;
        this.status = "UPLOADED";
    }
}