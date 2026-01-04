package com.beno.summaryspherebackend.entities;

import com.beno.summaryspherebackend.enums.SummaryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_summaries")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DocumentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    private String summaryType;

    @Column(columnDefinition = "TEXT")
    private String summaryText;

    @Enumerated(EnumType.STRING)
    private SummaryStatus status = SummaryStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
}