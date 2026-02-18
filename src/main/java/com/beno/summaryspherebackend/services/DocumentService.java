package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.Document;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface DocumentService {
    String storeFile(MultipartFile file, String title) throws IOException;
    Optional<Document> getDocumentById(String id);
    List<DocumentListDTO> listFiles();
    void deleteFile(String id) throws IOException;
    Resource loadFileAsResource(String id);
}