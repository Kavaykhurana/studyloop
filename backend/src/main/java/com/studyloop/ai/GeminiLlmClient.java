package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final long RETRY_DELAY_MS = 800;

    private enum Kind { TRANSIENT, BAD_OUTPUT, CONFIG }

    private static final class Failure extends RuntimeException {
        final transient Kind kind;
        final String code;

        Failure(Kind kind, String code) {
            super(code, null, false, false);
            this.kind = kind;
            this.code = code;
        }
    }

    private final GeminiModels models;
    private final GeminiTransport transport;
    private final JsonMapper mapper;
    private final AiCallLogger callLogger;

    public GeminiLlmClient(AppProperties props, GeminiTransport transport, JsonMapper mapper,
                           AiCallLogger callLogger) {
        this(new GeminiModels(props.ai()), transport, mapper, callLogger);
    }

    public GeminiLlmClient(GeminiModels models, GeminiTransport transport, JsonMapper mapper,
                           AiCallLogger callLogger) {
        this.models = models;
        this.transport = transport;
        this.mapper = mapper;
        this.callLogger = callLogger;
    }

    @Override
    public <T> AiResult<T> generateJson(AiRequest request, Class<T> type) {
        String primary = request.tier() == ModelTier.CHAT ? models.chat() : models.fast();
        String fallback = models.fast();
        Failure failure;
        try {
            return runWithRetry(primary, request, type);
        } catch (Failure f) {
            failure = f;
        }
        if (failure.kind == Kind.TRANSIENT && request.tier() == ModelTier.CHAT && !fallback.equals(primary)) {
            try {
                return attempt(fallback, request, type);
            } catch (Failure f) {
                failure = f;
            }
        }
        throw new AiUnavailableException(failure.code);
    }

    private <T> AiResult<T> runWithRetry(String model, AiRequest request, Class<T> type) {
        Failure last = null;
        for (int i = 0; i < 2; i++) {
            try {
                return attempt(model, request, type);
            } catch (Failure f) {
                last = f;
                if (f.kind == Kind.CONFIG) {
                    throw f;
                }
                if (i == 0 && f.kind == Kind.TRANSIENT) {
                    sleep(RETRY_DELAY_MS);
                }
            }
        }
        throw last;
    }

    private <T> AiResult<T> attempt(String model, AiRequest request, Class<T> type) {
        long start = System.nanoTime();
        Integer inTokens = null;
        Integer outTokens = null;
        try {
            String raw = transport.post("/models/" + model + ":generateContent", body(request), request.timeout());
            JsonNode root = mapper.readTree(raw);
            JsonNode usage = root.path("usageMetadata");
            inTokens = usage.has("promptTokenCount") ? usage.path("promptTokenCount").asInt() : null;
            outTokens = usage.has("candidatesTokenCount") ? usage.path("candidatesTokenCount").asInt() : null;
            T value = parse(root, type);
            long ms = elapsedMs(start);
            callLogger.log(request.feature(), model, ms, true, null, inTokens, outTokens);
            return new AiResult<>(value, model, ms);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            Failure f;
            if (status == 429 || status >= 500) {
                f = new Failure(Kind.TRANSIENT, "HTTP_" + status);
            } else if (status == 400 || status == 403 || status == 404) {
                log.error("Gemini rejected the request (HTTP {}); check the model name and API key for {}", status,
                        model);
                f = new Failure(Kind.CONFIG, "AI_CONFIG");
            } else {
                f = new Failure(Kind.BAD_OUTPUT, "HTTP_" + status);
            }
            callLogger.log(request.feature(), model, elapsedMs(start), false, f.code, inTokens, outTokens);
            throw f;
        } catch (ResourceAccessException e) {
            callLogger.log(request.feature(), model, elapsedMs(start), false, "TIMEOUT", inTokens, outTokens);
            throw new Failure(Kind.TRANSIENT, "TIMEOUT");
        } catch (Failure f) {
            callLogger.log(request.feature(), model, elapsedMs(start), false, f.code, inTokens, outTokens);
            throw f;
        } catch (JacksonException e) {
            callLogger.log(request.feature(), model, elapsedMs(start), false, "BAD_JSON", inTokens, outTokens);
            throw new Failure(Kind.BAD_OUTPUT, "BAD_JSON");
        }
    }

    private <T> T parse(JsonNode root, Class<T> type) {
        JsonNode candidate = root.path("candidates").path(0);
        if (candidate.isMissingNode() || candidate.isNull()) {
            throw new Failure(Kind.BAD_OUTPUT, "NO_CANDIDATE");
        }
        String finish = candidate.path("finishReason").asString("");
        if ("SAFETY".equals(finish) || "RECITATION".equals(finish)) {
            throw new Failure(Kind.BAD_OUTPUT, finish);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.path("thought").asBoolean(false) || !part.has("text")) {
                continue;
            }
            text.append(part.path("text").asString(""));
        }
        String json = stripFence(text.toString().trim());
        if (json.isEmpty()) {
            throw new Failure(Kind.BAD_OUTPUT, "EMPTY_OUTPUT");
        }
        try {
            return mapper.readValue(json, type);
        } catch (JacksonException e) {
            throw new Failure(Kind.BAD_OUTPUT, "BAD_JSON");
        }
    }

    private static String stripFence(String s) {
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            s = firstNewline < 0 ? "" : s.substring(firstNewline + 1);
            int end = s.lastIndexOf("```");
            if (end >= 0) {
                s = s.substring(0, end);
            }
        }
        return s.trim();
    }

    private String body(AiRequest r) {
        List<Object> parts = new ArrayList<>();
        parts.add(Map.of("text", r.userText()));
        if (r.imageBytes() != null) {
            parts.add(Map.of("inline_data", Map.of("mime_type", r.imageMimeType(),
                    "data", Base64.getEncoder().encodeToString(r.imageBytes()))));
        }
        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("responseMimeType", "application/json");
        generation.put("responseJsonSchema", r.responseJsonSchema());
        generation.put("maxOutputTokens", r.maxOutputTokens());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", r.systemPrompt()))));
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        body.put("generationConfig", generation);
        return mapper.writeValueAsString(body);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
