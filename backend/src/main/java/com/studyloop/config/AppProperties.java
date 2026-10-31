package com.studyloop.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cookie cookie,
        Cors cors,
        Ai ai,
        Similarity similarity,
        Similarity fakeSimilarity,
        Demo demo,
        Version version) {

    /**
     * Thresholds in force. The deterministic fake embedder produces a much lower similarity scale than Gemini, so when
     * no API key is configured the separately calibrated {@code app.fake-similarity} set applies instead.
     */
    @Override
    public Similarity similarity() {
        if (ai != null && !ai.enabled() && fakeSimilarity != null) {
            return fakeSimilarity;
        }
        return similarity;
    }

    public record Jwt(String secret, int accessTtlMinutes, int refreshTtlDays) {}

    public record Cookie(boolean secure) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Ai(String apiKey, String baseUrl, String chatModel, String fastModel, String embedModel) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Similarity(double duplicateThreshold, double groundingThreshold, double semanticFloor,
                             double semanticCeil) {}

    public record Demo(boolean enabled, boolean seed, String adminToken) {}

    public record Version(String gitSha) {}
}
