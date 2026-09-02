package com.beno.summaryspherebackend.dtos;

import jakarta.validation.constraints.NotBlank;
import com.beno.summaryspherebackend.enums.SummaryStatus;

public class SummarizationSchema {

    public SummarizationSchema() {
    }

    public record SummarizeRequest(
            @NotBlank(message = "Summary type is required")
            String summaryType
    ) {}
    public record SummarizeAcceptedResponse(Long summaryId, SummaryStatus status) {}
}
