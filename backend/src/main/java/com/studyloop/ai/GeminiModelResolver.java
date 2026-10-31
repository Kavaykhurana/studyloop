package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs first at startup (before seeding and embedding checks): lists the Gemini models once and resolves every
 * configured id against it. Never fails startup; a missing model only logs a warning.
 */
@Component
public class GeminiModelResolver implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GeminiModelResolver.class);

    private final AppProperties props;
    private final GeminiTransport transport;
    private final JsonMapper mapper;
    private final GeminiModels models;

    public GeminiModelResolver(AppProperties props, GeminiTransport transport, JsonMapper mapper,
                               GeminiModels models) {
        this.props = props;
        this.transport = transport;
        this.mapper = mapper;
        this.models = models;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.ai().enabled()) {
            return;
        }
        List<GeminiModels.Listed> available;
        try {
            available = list();
        } catch (RuntimeException e) {
            log.warn("Could not verify Gemini models at startup: {}", e.getMessage());
            return;
        }
        String chat = pick(props.ai().chatModel(), "generateContent", available);
        String fast = pick(props.ai().fastModel(), "generateContent", available);
        String embed = pick(props.ai().embedModel(), "embedContent", available);
        models.use(chat, fast, embed);
        log.info("Gemini models in use: chat={}, fast={}, embed={}", chat, fast, embed);
    }

    private String pick(String configured, String method, List<GeminiModels.Listed> available) {
        return GeminiModels.resolve(configured, method, available).map(resolved -> {
            if (!resolved.equals(configured)) {
                log.warn("Configured model {} not found in Gemini model list; using {}", configured, resolved);
            }
            return resolved;
        }).orElseGet(() -> {
            log.warn("Configured model {} not found in Gemini model list", configured);
            return configured;
        });
    }

    private List<GeminiModels.Listed> list() {
        List<GeminiModels.Listed> out = new ArrayList<>();
        String pageToken = null;
        for (int page = 0; page < 5; page++) {
            String path = "/models?pageSize=200"
                    + (pageToken == null ? "" : "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(transport.get(path, Duration.ofSeconds(5)));
            root.path("models").forEach(m -> {
                List<String> methods = new ArrayList<>();
                m.path("supportedGenerationMethods").forEach(x -> methods.add(x.asString("")));
                out.add(new GeminiModels.Listed(m.path("name").asString("").replace("models/", ""), methods));
            });
            pageToken = root.path("nextPageToken").asString("");
            if (pageToken.isEmpty()) {
                break;
            }
        }
        return out;
    }
}
