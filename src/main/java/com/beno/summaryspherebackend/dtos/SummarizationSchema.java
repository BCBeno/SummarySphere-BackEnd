package com.beno.summaryspherebackend.dtos;

public class SummarizationSchema {

    public SummarizationSchema() {
    }

    public record SummarizeRequest(String summaryType) {}
    public record SummarizeResponse(String message, String documentId) {}
}
