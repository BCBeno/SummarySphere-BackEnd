package com.beno.summaryspherebackend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DocumentVectorService {

    private static final Logger log = LoggerFactory.getLogger(DocumentVectorService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public DocumentVectorService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Splits the document text into chunks, generates embeddings, and stores them
     * in the vector database.
     * Each chunk is tagged with the documentId in its metadata for filtering during
     * search and deletion.
     */
    public void ingestDocument(String documentId, String content) {
        Document springAiDoc = new Document(content, Map.of("documentId", documentId));

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .withPunctuationMarks(List.of('.', '?', '!', '\n'))
                .build();
        List<Document> chunks = splitter.apply(List.of(springAiDoc));

        log.info("Document {} split into {} chunks for vector embedding", documentId, chunks.size());

        vectorStore.add(chunks);

        log.info("Document {} successfully ingested into vector store", documentId);
    }

    public List<String> searchRelevantChunks(String documentId, String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression("documentId == '" + documentId + "'")
                .similarityThreshold(0.3)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("Vector search for document {} returned {} chunks", documentId, results.size());

        return results.stream()
                .map(Document::getText)
                .toList();
    }

    /**
     * Deletes all vector store entries for a given document.
     * Uses a direct JDBC query since PgVectorStore doesn't support metadata-based
     * bulk delete.
     */
    public void deleteDocumentChunks(String documentId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'documentId' = ?",
                documentId);
        log.info("Deleted {} vector chunks for document {}", deleted, documentId);
    }
}
