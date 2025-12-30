package com.beno.summaryspherebackend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {
    @Id
    private String documentId;
    private String originalFilename;
    private Long size;
    private String fileType;
    private String status;
    private LocalDateTime uploadedAt;
    @Column(columnDefinition = "TEXT")
    private String content;
    //private String uploadedBy;

    public Document(String documentId, String originalFilename, Long size, String fileType, String content) {
        this.documentId = documentId;
        this.originalFilename = originalFilename;
        this.size = size;
        this.uploadedAt = LocalDateTime.now();
        this.fileType = fileType;
        this.content = content;
        this.status = "UPLOADED";
    }
}
