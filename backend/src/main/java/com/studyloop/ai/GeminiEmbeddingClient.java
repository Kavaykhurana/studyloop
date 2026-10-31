package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import com.studyloop.embedding.VectorMath;
import java.time.Duration;
import java.util.ArrayList;
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

public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);
    private static final int DIMENSIONS = 768;
    private static final int MAX_BATCH = 50;

    private final AppProperties.Ai ai;
    private final GeminiTransport transport;
    private final JsonMapper mapper;
    private final AiCallLogger callLogger;

    public GeminiEmbeddingClient(AppProperties props, GeminiTransport transport, JsonMapper mapper,
                                 AiCallLogger callLogger) {
        this.ai = props.ai();
        this.transport = transport;
        this.mapper = mapper;
        this.callLogger = callLogger;
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", Map.of("parts", List.of(Map.of("text", text))));
        body.put("outputDimensionality", DIMENSIONS);
        JsonNode root = call("/models/" + ai.embedModel() + ":embedContent", body, Duration.ofSeconds(5));
        return toVector(root.path("embedding").path("values"));
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts.size() > MAX_BATCH) {
            throw new IllegalArgumentException("embedBatch accepts at most " + MAX_BATCH + " texts");
        }
        if (texts.isEmpty()) {
            return List.of();
        }
        List<Object> requests = new ArrayList<>();
        for (String text : texts) {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("model", "models/" + ai.embedModel());
            req.put("content", Map.of("parts", List.of(Map.of("text", text))));
            req.put("outputDimensionality", DIMENSIONS);
            requests.add(req);
        }
        JsonNode root = call("/models/" + ai.embedModel() + ":batchEmbedContents", Map.of("requests", requests),
                Duration.ofSeconds(20));
        JsonNode embeddings = root.path("embeddings");
        if (!embeddings.isArray() || embeddings.size() != texts.size()) {
            throw new AiUnavailableException("EMBED_COUNT");
        }
        List<float[]> out = new ArrayList<>();
        for (JsonNode e : embeddings) {
            out.add(toVector(e.path("values")));
        }
        return out;
    }

    private JsonNode call(String path, Map<String, Object> body, Duration timeout) {
        String json = mapper.writeValueAsString(body);
        for (int attempt = 0; attempt < 2; attempt++) {
            long start = System.nanoTime();
            try {
                String raw = transport.post(path, json, timeout);
                JsonNode root = mapper.readTree(raw);
                callLogger.log(AiFeature.EMBED, ai.embedModel(), elapsedMs(start), true, null, null, null);
                return root;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                callLogger.log(AiFeature.EMBED, ai.embedModel(), elapsedMs(start), false, "HTTP_" + status, null,
                        null);
                if ((status == 429 || status == 503) && attempt == 0) {
                    sleep();
                    continue;
                }
                if (status == 400 || status == 403) {
                    log.error("Gemini rejected the embedding request (HTTP {}); check GEMINI_EMBED_MODEL", status);
                    throw new AiUnavailableException("AI_CONFIG");
                }
                throw new AiUnavailableException("HTTP_" + status);
            } catch (ResourceAccessException e) {
                callLogger.log(AiFeature.EMBED, ai.embedModel(), elapsedMs(start), false, "TIMEOUT", null, null);
                throw new AiUnavailableException("TIMEOUT", e);
            } catch (JacksonException e) {
                throw new AiUnavailableException("BAD_JSON", e);
            }
        }
        throw new AiUnavailableException("EMBED_FAILED");
    }

    private static float[] toVector(JsonNode values) {
        if (!values.isArray() || values.size() != DIMENSIONS) {
            throw new AiUnavailableException("EMBED_DIM");
        }
        float[] v = new float[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            v[i] = (float) values.get(i).asDouble();
        }
        return VectorMath.normalize(v);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static void sleep() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
