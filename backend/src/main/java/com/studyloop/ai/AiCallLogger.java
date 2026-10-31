package com.studyloop.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes one ai_calls row per model attempt. Never throws and never records prompts or outputs. */
@Component
public class AiCallLogger {

    private static final Logger log = LoggerFactory.getLogger(AiCallLogger.class);

    private final JdbcTemplate jdbc;

    public AiCallLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AiFeature feature, String model, long latencyMs, boolean success, String errorCode,
                    Integer inputTokens, Integer outputTokens) {
        log.info("ai call feature={} model={} latencyMs={} success={}", feature, model, latencyMs, success);
        try {
            jdbc.update("INSERT INTO ai_calls (feature, model, latency_ms, success, error_code, input_tokens, "
                            + "output_tokens) VALUES (?, ?, ?, ?, ?, ?, ?)", feature.name(), model,
                    (int) Math.min(latencyMs, Integer.MAX_VALUE), success, errorCode, inputTokens, outputTokens);
        } catch (RuntimeException e) {
            log.warn("could not record ai call: {}", e.getMessage());
        }
    }
}
