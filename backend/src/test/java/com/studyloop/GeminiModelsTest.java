package com.studyloop.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiModelsTest {

    private static final List<String> GENERATE = List.of("generateContent", "countTokens");
    private static final List<String> EMBED = List.of("embedContent");

    private static final List<GeminiModels.Listed> AVAILABLE = List.of(
            new GeminiModels.Listed("gemini-3.5-flash", GENERATE),
            new GeminiModels.Listed("gemini-3.1-flash-lite-preview", GENERATE),
            new GeminiModels.Listed("gemini-embedding-2-preview", EMBED),
            new GeminiModels.Listed("gemini-embedding-001", EMBED),
            new GeminiModels.Listed("gemini-4-pro-001", GENERATE),
            new GeminiModels.Listed("gemini-4-pro-preview-0901", GENERATE));

    @Test
    void anAvailableConfiguredModelIsKept() {
        assertThat(GeminiModels.resolve("gemini-3.5-flash", "generateContent", AVAILABLE)).contains("gemini-3.5-flash");
    }

    @Test
    void aMissingModelFallsBackToItsPreviewVariantAndBack() {
        assertThat(GeminiModels.resolve("gemini-embedding-2", "embedContent", AVAILABLE))
                .contains("gemini-embedding-2-preview");
        assertThat(GeminiModels.resolve("gemini-3.1-flash-lite", "generateContent", AVAILABLE))
                .contains("gemini-3.1-flash-lite-preview");
        assertThat(GeminiModels.resolve("gemini-3.5-flash-preview", "generateContent", AVAILABLE))
                .contains("gemini-3.5-flash");
    }

    @Test
    void otherwiseTheShortestListedVariantOfTheSameModelIsUsed() {
        assertThat(GeminiModels.resolve("gemini-4-pro", "generateContent", AVAILABLE)).contains("gemini-4-pro-001");
    }

    @Test
    void theRequiredMethodMustBeSupportedAndUnknownModelsStayUnresolved() {
        assertThat(GeminiModels.resolve("gemini-3.5-flash", "embedContent", AVAILABLE)).isEmpty();
        assertThat(GeminiModels.resolve("gemini-9-ultra", "generateContent", AVAILABLE)).isEmpty();
    }
}
