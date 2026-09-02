package com.beno.summaryspherebackend.services.agent;

import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.DocumentSummary;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.DocumentRepository;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.DocumentSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Optional;

@Component
public class AgentTools {

    private static final Logger log = LoggerFactory.getLogger(AgentTools.class);

    private final DocumentService documentService;
    private final DocumentSummaryService documentSummaryService;
    private final DocumentRepository documentRepository;

    public AgentTools(DocumentService documentService,
            DocumentSummaryService documentSummaryService,
            DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentSummaryService = documentSummaryService;
        this.documentRepository = documentRepository;
    }

    private User requireUser(ToolContext context) {
        if (context == null || context.getContext() == null) {
            throw new IllegalStateException("ToolContext is missing.");
        }
        User user = (User) context.getContext().get("currentUser");
        if (user == null) {
            throw new IllegalStateException("No authenticated user context available for tool execution.");
        }
        return user;
    }

    @Tool(description = "List all documents belonging to the current user. " +
            "Returns each document's ID, title, original filename, file type, upload date, and status. " +
            "Use this to find documents before reading their content or generating summaries.")
    public String listUserDocuments(ToolContext context) {
        User user = requireUser(context);
        List<Document> documents = documentRepository.findByUploadedBy(user);
        log.info("Agent tool [listUserDocuments] called for user={}, found {} documents", user.getEmail(),
                documents.size());

        if (documents.isEmpty()) {
            return "No documents found. The user hasn't uploaded any documents yet.";
        }

        StringBuilder sb = new StringBuilder("Found " + documents.size() + " document(s):\n\n");
        for (Document doc : documents) {
            sb.append("- ID: ").append(doc.getDocumentId())
                    .append(" | Title: ").append(doc.getTitle())
                    .append(" | File: ").append(doc.getOriginalFilename())
                    .append(" | Type: ").append(doc.getFileType())
                    .append(" | Uploaded: ").append(doc.getUploadedAt())
                    .append(" | Status: ").append(doc.getStatus())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Search the current user's documents by a keyword. " +
            "Searches in document titles and original filenames. " +
            "Returns matching document IDs, titles, and metadata.")
    public String searchDocuments(
            @ToolParam(description = "The keyword to search for in document titles and filenames") String query,
            ToolContext context) {
        User user = requireUser(context);
        log.info("Agent tool [searchDocuments] called: query='{}', user={}", query, user.getEmail());

        List<Document> allDocs = documentRepository.findByUploadedBy(user);
        String lowerQuery = query.toLowerCase();

        List<Document> matches = allDocs.stream()
                .filter(doc -> {
                    String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";
                    String filename = doc.getOriginalFilename() != null ? doc.getOriginalFilename().toLowerCase() : "";
                    return title.contains(lowerQuery) || filename.contains(lowerQuery);
                })
                .toList();

        if (matches.isEmpty()) {
            return "No documents found matching '" + query + "'.";
        }

        StringBuilder sb = new StringBuilder("Found " + matches.size() + " document(s) matching '" + query + "':\n\n");
        for (Document doc : matches) {
            sb.append("- ID: ").append(doc.getDocumentId())
                    .append(" | Title: ").append(doc.getTitle())
                    .append(" | File: ").append(doc.getOriginalFilename())
                    .append(" | Uploaded: ").append(doc.getUploadedAt())
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get the full extracted text content of a specific document by its ID. " +
            "Use this when you need to read, analyze, or compare document contents. " +
            "The content may be long for large documents.")
    public String getDocumentContent(
            @ToolParam(description = "The exact UUID of the document. Do NOT pass a title here. Use searchDocuments first if you only have a title.") String documentId,
            ToolContext context) {
        User user = requireUser(context);
        log.info("Agent tool [getDocumentContent] called: docId='{}', user={}", documentId, user.getEmail());

        Optional<Document> docOpt;
        try {
            docOpt = documentService.getDocumentById(documentId);
        } catch (Exception e) {
            return "Error: Invalid document ID format. Please use searchDocuments to find the correct UUID.";
        }

        if (docOpt.isEmpty()) {
            return "Document not found with ID: " + documentId;
        }

        Document doc = docOpt.get();
        if (!isOwner(doc, user)) {
            return "You don't have access to this document.";
        }

        String content = doc.getContent();
        if (content == null || content.isBlank()) {
            return "Document '" + doc.getTitle() + "' has no extracted text content.";
        }

        int maxLength = 15000;
        if (content.length() > maxLength) {
            return "Document '" + doc.getTitle() + "' content (truncated to first " + maxLength + " characters):\n\n"
                    + content.substring(0, maxLength) + "\n\n[... content truncated, " + content.length()
                    + " total characters]";
        }

        return "Document '" + doc.getTitle() + "' content:\n\n" + content;
    }

    @Tool(description = "Get an existing summary for a document. " +
            "Summary types include: 'brief', 'detailed', 'bullet', 'academic'. " +
            "Returns the summary text if available, or indicates no summary exists. " +
            "If no summary exists, consider using triggerSummarization to create one.")
    public String getDocumentSummary(
            @ToolParam(description = "The exact UUID of the document. Do NOT pass a title here. Use searchDocuments first if you only have a title.") String documentId,
            @ToolParam(description = "The summary type: brief, detailed, bullet, or academic") String summaryType,
            ToolContext context) {
        User user = requireUser(context);
        log.info("Agent tool [getDocumentSummary] called: docId='{}', type='{}', user={}", documentId, summaryType,
                user.getEmail());

        Optional<Document> docOpt;
        try {
            docOpt = documentService.getDocumentById(documentId);
        } catch (Exception e) {
            return "Error: Invalid document ID format. Please use searchDocuments to find the correct UUID.";
        }
        if (docOpt.isEmpty()) {
            return "Document not found with ID: " + documentId;
        }
        if (!isOwner(docOpt.get(), user)) {
            return "You don't have access to this document.";
        }

        Optional<DocumentSummary> summaryOpt = documentSummaryService.getLatestSummaryForDocumentByType(documentId,
                summaryType);
        if (summaryOpt.isEmpty()) {
            return "No '" + summaryType + "' summary exists for document '" + docOpt.get().getTitle()
                    + "'. You can create one using the triggerSummarization tool.";
        }

        DocumentSummary summary = summaryOpt.get();
        String summaryText = summary.getSummaryText();
        if (summaryText == null || summaryText.isBlank()) {
            return "Summary exists but has no content (status: " + summary.getStatus() + ").";
        }

        return "Summary (type: " + summaryType + ") for '" + docOpt.get().getTitle() + "':\n\n" + summaryText;
    }

    @Tool(description = "Trigger AI summarization for a document. " +
            "This creates a new summary of the specified type. " +
            "Only call this if getDocumentSummary returned no existing summary. " +
            "Available types: 'concise', 'detailed', 'bullet points'. " +
            "This operation may take a few seconds to complete.")
    public String triggerSummarization(
            @ToolParam(description = "The exact UUID of the document. Do NOT pass a title here. Use searchDocuments first if you only have a title.") String documentId,
            @ToolParam(description = "The summary type: brief, detailed, bullet, or academic") String summaryType,
            ToolContext context) {
        User user = requireUser(context);
        log.info("Agent tool [triggerSummarization] called: docId='{}', type='{}', user={}", documentId, summaryType,
                user.getEmail());

        Optional<Document> docOpt;
        try {
            docOpt = documentService.getDocumentById(documentId);
        } catch (Exception e) {
            return "Error: Invalid document ID format. Please use searchDocuments to find the correct UUID.";
        }
        if (docOpt.isEmpty()) {
            return "Document not found with ID: " + documentId;
        }
        if (!isOwner(docOpt.get(), user)) {
            return "You don't have access to this document.";
        }

        try {
            DocumentSummary summary = documentSummaryService.requestSummary(documentId, summaryType, user);
            return "Summary generation accepted for '" + docOpt.get().getTitle()
                    + "' (summary ID: " + summary.getId() + ", status: " + summary.getStatus() + ").";
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.error("Agent tool [triggerSummarization] failed: docId={}, type={}", documentId, summaryType, ex);
            return "Failed to generate summary: " + cause.getMessage();
        }
    }

    @Tool(description = "Generate a quiz with questions based on a document's content. " +
            "Returns formatted quiz questions with multiple-choice answers. " +
            "Useful for helping students test their knowledge of the material.")
    public String generateQuiz(
            @ToolParam(description = "The exact UUID of the document. Do NOT pass a title here. Use searchDocuments first if you only have a title.") String documentId,
            @ToolParam(description = "Number of quiz questions to generate (1-20)") int numberOfQuestions,
            ToolContext context) {
        User user = requireUser(context);
        log.info("Agent tool [generateQuiz] called: docId='{}', numQuestions={}, user={}", documentId,
                numberOfQuestions, user.getEmail());

        Optional<Document> docOpt;
        try {
            docOpt = documentService.getDocumentById(documentId);
        } catch (Exception e) {
            return "Error: Invalid document ID format. Please use searchDocuments to find the correct UUID.";
        }
        if (docOpt.isEmpty()) {
            return "Document not found with ID: " + documentId;
        }
        if (!isOwner(docOpt.get(), user)) {
            return "You don't have access to this document.";
        }

        String content = docOpt.get().getContent();
        if (content == null || content.isBlank()) {
            return "Document '" + docOpt.get().getTitle() + "' has no extracted text content to generate a quiz from.";
        }

        int clampedCount = Math.max(1, Math.min(numberOfQuestions, 20));

        int maxLength = 12000;
        String truncatedContent = content.length() > maxLength
                ? content.substring(0, maxLength) + "\n[... truncated]"
                : content;

        return "Document content for '" + docOpt.get().getTitle() + "'. Please generate a " + clampedCount
                + "-question quiz based on this text:\n\n" + truncatedContent;
    }

    private boolean isOwner(Document doc, User user) {
        if (doc == null || user == null)
            return false;
        User owner = doc.getUploadedBy();
        if (owner == null)
            return false;
        return owner.getId() != null && owner.getId().equals(user.getId());
    }
}
