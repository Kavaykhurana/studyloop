package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Picks Gemini implementations when GEMINI_API_KEY is set, deterministic fakes otherwise. */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public GeminiModels geminiModels(AppProperties props) {
        return new GeminiModels(props.ai());
    }

    @Bean
    public LlmClient llmClient(AppProperties props, GeminiModels models, GeminiTransport transport,
                               JsonMapper mapper, AiCallLogger callLogger) {
        if (props.ai().enabled()) {
            return new GeminiLlmClient(models, transport, mapper, callLogger);
        }
        log.warn("GEMINI_API_KEY not set — running with deterministic fake AI");
        return new FakeLlmClient(mapper);
    }

    @Bean
    public EmbeddingClient embeddingClient(AppProperties props, GeminiModels models, GeminiTransport transport,
                                           JsonMapper mapper, AiCallLogger callLogger) {
        if (props.ai().enabled()) {
            return new GeminiEmbeddingClient(models, transport, mapper, callLogger);
        }
        return new FakeEmbeddingClient();
    }
}
