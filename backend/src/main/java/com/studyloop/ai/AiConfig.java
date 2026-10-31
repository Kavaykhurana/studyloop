package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Picks Gemini implementations when GEMINI_API_KEY is set, deterministic fakes otherwise. */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public LlmClient llmClient(AppProperties props, GeminiTransport transport, JsonMapper mapper,
                               AiCallLogger callLogger) {
        if (props.ai().enabled()) {
            return new GeminiLlmClient(props, transport, mapper, callLogger);
        }
        log.warn("GEMINI_API_KEY not set — running with deterministic fake AI");
        return new FakeLlmClient(mapper);
    }

    @Bean
    public EmbeddingClient embeddingClient(AppProperties props, GeminiTransport transport, JsonMapper mapper,
                                           AiCallLogger callLogger) {
        if (props.ai().enabled()) {
            return new GeminiEmbeddingClient(props, transport, mapper, callLogger);
        }
        return new FakeEmbeddingClient();
    }

    /** Warns (never fails) when a configured model name is not in Gemini's model list. */
    @Bean
    public ApplicationRunner geminiModelCheck(AppProperties props, GeminiTransport transport, JsonMapper mapper) {
        return args -> {
            if (!props.ai().enabled()) {
                return;
            }
            try {
                JsonNode root = mapper.readTree(transport.get("/models?pageSize=200", Duration.ofSeconds(5)));
                List<String> available = new ArrayList<>();
                root.path("models").forEach(m -> available.add(m.path("name").asString("").replace("models/", "")));
                for (String name : List.of(props.ai().chatModel(), props.ai().fastModel(),
                        props.ai().embedModel())) {
                    if (!available.contains(name)) {
                        log.warn("Configured model {} not found in Gemini model list", name);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Could not verify Gemini models at startup: {}", e.getMessage());
            }
        };
    }
}
