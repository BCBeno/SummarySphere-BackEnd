package com.beno.summaryspherebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingAutoConfiguration;

public class DebugConditionsTest {

    @Test
    public void testConditions() {
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GoogleGenAiEmbeddingAutoConfiguration.class))
            .withPropertyValues(
                "spring.ai.model.embedding.text=google-genai",
                "spring.ai.google.genai.api-key=dummy"
            );

        contextRunner.run(context -> {
            if (context.containsBean("googleGenAiEmbeddingModel")) {
                System.out.println("DEBUG: BEAN CREATED SUCCESSFULLY");
            } else {
                System.out.println("DEBUG: BEAN NOT CREATED");
            }
        });
    }
}
