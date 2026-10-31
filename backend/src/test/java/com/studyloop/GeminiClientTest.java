package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpServer;
import com.studyloop.ai.AiCallLogger;
import com.studyloop.ai.AiFeature;
import com.studyloop.ai.AiRequest;
import com.studyloop.ai.AiResult;
import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.GeminiEmbeddingClient;
import com.studyloop.ai.GeminiLlmClient;
import com.studyloop.ai.GeminiTransport;
import com.studyloop.ai.ModelTier;
import com.studyloop.ai.dto.ClassifyResult;
import com.studyloop.config.AppProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Exercises the real Gemini client code against a local stand-in server, covering retry and fallback policy. */
class GeminiClientTest {

    private record Reply(int status, String body) {}

    private HttpServer server;
    private final List<String> paths = new CopyOnWriteArrayList<>();
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> keys = new CopyOnWriteArrayList<>();
    private Function<String, Reply> script = path -> new Reply(200, "{}");
    private final JsonMapper mapper = JsonMapper.builder().build();
    private GeminiLlmClient llm;
    private GeminiEmbeddingClient embeddings;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            paths.add(path);
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            keys.add(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            Reply reply = script.apply(path);
            byte[] out = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        AppProperties props = new AppProperties(null, null, null,
                new AppProperties.Ai("test-key", "http://127.0.0.1:" + server.getAddress().getPort(),
                        "chat-model", "fast-model", "embed-model"), null, null, null, null);
        GeminiTransport transport = new GeminiTransport(HttpClient.newHttpClient(), props);
        AiCallLogger logger = mock(AiCallLogger.class);
        llm = new GeminiLlmClient(props, transport, mapper, logger);
        embeddings = new GeminiEmbeddingClient(props, transport, mapper, logger);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private AiRequest request(ModelTier tier) {
        return new AiRequest(AiFeature.CLASSIFY, tier, "system prompt", "user text", null, null,
                Map.of("type", "object"), 256, Duration.ofSeconds(5));
    }

    private static String candidate(String text) {
        return "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":["
                + "{\"thought\":true,\"text\":\"private reasoning\"},{\"text\":" + quote(text) + "}]}}],"
                + "\"usageMetadata\":{\"promptTokenCount\":11,\"candidatesTokenCount\":7}}";
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static final String CLASSIFY_JSON = "{\"subjectCode\":\"DBMS\",\"topic\":\"Normalization\","
            + "\"difficulty\":2,\"isAcademic\":true,\"suggestedTitle\":\"A clear title\"}";

    @Test
    void parsesStructuredOutputSkipsThoughtPartsAndStripsCodeFences() {
        script = path -> new Reply(200, candidate("```json\n" + CLASSIFY_JSON + "\n```"));

        AiResult<ClassifyResult> result = llm.generateJson(request(ModelTier.FAST), ClassifyResult.class);

        assertThat(result.value().subjectCode()).isEqualTo("DBMS");
        assertThat(result.value().isAcademic()).isTrue();
        assertThat(result.model()).isEqualTo("fast-model");
        assertThat(paths).containsExactly("/models/fast-model:generateContent");
        assertThat(keys).containsExactly("test-key");
    }

    @Test
    void requestBodyCarriesSchemaAndSystemPromptButNoSamplingParameters() {
        script = path -> new Reply(200, candidate(CLASSIFY_JSON));

        llm.generateJson(request(ModelTier.CHAT), ClassifyResult.class);

        String body = bodies.get(0);
        assertThat(body).contains("\"systemInstruction\"").contains("system prompt").contains("user text")
                .contains("\"responseMimeType\":\"application/json\"").contains("\"responseJsonSchema\"")
                .contains("\"maxOutputTokens\":256");
        assertThat(body).doesNotContain("temperature").doesNotContain("topP").doesNotContain("topK")
                .doesNotContain("inline_data");
    }

    @Test
    void imagesAreSentAsInlineData() {
        script = path -> new Reply(200, candidate(CLASSIFY_JSON));
        AiRequest withImage = new AiRequest(AiFeature.SNAP, ModelTier.CHAT, "s", "u", new byte[] {1, 2, 3},
                "image/png", Map.of(), 1024, Duration.ofSeconds(5));

        llm.generateJson(withImage, ClassifyResult.class);

        assertThat(bodies.get(0)).contains("\"inline_data\"").contains("\"mime_type\":\"image/png\"")
                .contains("AQID");
    }

    @Test
    void aTransientErrorIsRetriedOnceOnTheSameModel() {
        List<Integer> calls = new ArrayList<>();
        script = path -> {
            calls.add(1);
            return calls.size() == 1 ? new Reply(429, "{}") : new Reply(200, candidate(CLASSIFY_JSON));
        };

        AiResult<ClassifyResult> result = llm.generateJson(request(ModelTier.CHAT), ClassifyResult.class);

        assertThat(result.model()).isEqualTo("chat-model");
        assertThat(paths).containsExactly("/models/chat-model:generateContent", "/models/chat-model:generateContent");
    }

    @Test
    void whenTheChatModelKeepsFailingTheFastModelIsTriedOnce() {
        script = path -> path.contains("chat-model") ? new Reply(503, "{}") : new Reply(200, candidate(CLASSIFY_JSON));

        AiResult<ClassifyResult> result = llm.generateJson(request(ModelTier.CHAT), ClassifyResult.class);

        assertThat(result.model()).isEqualTo("fast-model");
        assertThat(paths).containsExactly("/models/chat-model:generateContent", "/models/chat-model:generateContent",
                "/models/fast-model:generateContent");
    }

    @Test
    void configurationErrorsAreNotRetried() {
        script = path -> new Reply(400, "{\"error\":\"model not found\"}");

        assertThatThrownBy(() -> llm.generateJson(request(ModelTier.CHAT), ClassifyResult.class))
                .isInstanceOfSatisfying(AiUnavailableException.class,
                        e -> assertThat(e.getCode()).isEqualTo("AI_CONFIG"));
        assertThat(paths).hasSize(1);
    }

    @Test
    void safetyBlocksAndUnparseableJsonAreRetriedOnceThenFail() {
        script = path -> new Reply(200, "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}");
        assertThatThrownBy(() -> llm.generateJson(request(ModelTier.FAST), ClassifyResult.class))
                .isInstanceOf(AiUnavailableException.class);
        assertThat(paths).hasSize(2);

        paths.clear();
        script = path -> new Reply(200, candidate("this is not json"));
        assertThatThrownBy(() -> llm.generateJson(request(ModelTier.FAST), ClassifyResult.class))
                .isInstanceOfSatisfying(AiUnavailableException.class,
                        e -> assertThat(e.getCode()).isEqualTo("BAD_JSON"));
        assertThat(paths).hasSize(2);
    }

    private static String vector(int dims, float first) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dims; i++) {
            sb.append(i == 0 ? first : 0.0f).append(i < dims - 1 ? "," : "");
        }
        return sb.append("]").toString();
    }

    @Test
    void singleEmbeddingRequestsTheConfiguredDimensionsAndNormalises() {
        script = path -> new Reply(200, "{\"embedding\":{\"values\":" + vector(768, 3f) + "}}");

        float[] v = embeddings.embed("hello");

        assertThat(v).hasSize(768);
        assertThat(v[0]).isEqualTo(1.0f);
        assertThat(paths).containsExactly("/models/embed-model:embedContent");
        assertThat(bodies.get(0)).contains("\"outputDimensionality\":768");
    }

    @Test
    void batchEmbeddingUsesTheBatchEndpointAndKeepsOrder() {
        script = path -> new Reply(200, "{\"embeddings\":[{\"values\":" + vector(768, 2f) + "},{\"values\":"
                + vector(768, 5f) + "}]}");

        List<float[]> out = embeddings.embedBatch(List.of("a", "b"));

        assertThat(out).hasSize(2);
        assertThat(paths).containsExactly("/models/embed-model:batchEmbedContents");
        assertThat(bodies.get(0)).contains("\"model\":\"models/embed-model\"");
    }

    @Test
    void aWrongDimensionIsRejected() {
        script = path -> new Reply(200, "{\"embedding\":{\"values\":" + vector(100, 1f) + "}}");

        assertThatThrownBy(() -> embeddings.embed("hello")).isInstanceOfSatisfying(AiUnavailableException.class,
                e -> assertThat(e.getCode()).isEqualTo("EMBED_DIM"));
    }
}
