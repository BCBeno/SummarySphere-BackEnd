package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByUploadedBy(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.documentId = :id")
    Optional<Document> findByIdForUpdate(@Param("id") String id);
}
