package com.beno.summaryspherebackend.dtos;

import jakarta.validation.constraints.NotBlank;

public class SummarizationSchema {

    public SummarizationSchema() {
    }

    public record SummarizeRequest(
            @NotBlank(message = "Summary type is required")
            String summaryType
    ) {}
    public record SummarizeResponse(String message, String documentId) {}
}
