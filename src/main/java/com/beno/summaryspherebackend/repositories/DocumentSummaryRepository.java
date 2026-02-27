package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSummaryRepository extends JpaRepository<DocumentSummary, Long> {
    List<DocumentSummary> findAllByDocument(Document document);
    Optional<DocumentSummary> findFirstByDocumentOrderByCreatedAtDesc(Document document);

}
