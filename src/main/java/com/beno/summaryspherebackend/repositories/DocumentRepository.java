package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByUploadedBy(User user);
}
