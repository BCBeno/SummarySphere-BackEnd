package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.enums.SummaryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface DocumentSummaryRepository extends JpaRepository<DocumentSummary, Long> {
    List<DocumentSummary> findAllByDocument(Document document);
    Optional<DocumentSummary> findFirstByDocumentOrderByCreatedAtDesc(Document document);

    Optional<DocumentSummary> findFirstByDocumentAndSummaryTypeIgnoreCaseOrderByCreatedAtDesc(Document document, String summaryType);
    List<DocumentSummary> findAllByDocumentOrderByCreatedAtDesc(Document document);

    boolean existsByDocumentAndSummaryTypeIgnoreCaseAndStatusIn(
            Document document,
            String summaryType,
            List<SummaryStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DocumentSummary s join fetch s.document where s.id = :id")
    Optional<DocumentSummary> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s.id from DocumentSummary s
            where (s.status = :pending and s.createdAt < :cutoff)
               or (s.status = :processing and s.lastAttemptAt < :cutoff)
            """)
    List<Long> findStuckSummaryIds(
            @Param("pending") SummaryStatus pending,
            @Param("processing") SummaryStatus processing,
            @Param("cutoff") LocalDateTime cutoff
    );
}
