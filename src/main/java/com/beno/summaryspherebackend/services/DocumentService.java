package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface DocumentService {
    String storeFile(MultipartFile file, String title, User uploader) throws IOException;
    Document getOwnedDocument(String id, String userId);
    List<DocumentListDTO> listFiles();
    List<DocumentListDTO> listFilesByUser(User user);
    void deleteOwnedDocument(String id, String userId);
    String createOwnedDownloadUrl(String id, String userId);
    void deleteFilesByUser(User user);

}