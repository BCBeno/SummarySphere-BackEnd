package com.beno.summaryspherebackend.entities;

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
@AllArgsConstructor
public class Document {
    @Id
    private String id;
    private String originalFilename;
    private Long size;
    private String uploadedAt;
    //private String uploadedBy;

    public Document(String id, String originalFilename, Long size) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.size = size;
        this.uploadedAt = LocalDateTime.now().toString();
    }
}
