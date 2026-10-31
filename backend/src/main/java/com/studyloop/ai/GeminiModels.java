package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The Gemini model ids actually used at runtime. They start as the configured names; at startup
 * {@link GeminiModelResolver} checks them against the live model list and, when a configured id is missing, switches to
 * the closest available variant of the same model (for example {@code gemini-embedding-2-preview} for
 * {@code gemini-embedding-2}) instead of letting every AI call fail on a renamed model.
 */
public class GeminiModels {

    /** A model as listed by {@code GET /models}: its id and the generation methods it supports. */
    public record Listed(String id, List<String> methods) {}

    private volatile String chat;
    private volatile String fast;
    private volatile String embed;
    private final String chatThinking;
    private final String fastThinking;

    public GeminiModels(AppProperties.Ai ai) {
        this.chat = ai.chatModel();
        this.fast = ai.fastModel();
        this.embed = ai.embedModel();
        this.chatThinking = ai.chatThinking() == null ? "" : ai.chatThinking().trim();
        this.fastThinking = ai.fastThinking() == null ? "" : ai.fastThinking().trim();
    }

    /** Configured thinking level for a tier; blank means "send no thinking config". */
    public String thinking(ModelTier tier) {
        return tier == ModelTier.CHAT ? chatThinking : fastThinking;
    }

    public String chat() {
        return chat;
    }

    public String fast() {
        return fast;
    }

    public String embed() {
        return embed;
    }

    void use(String chatModel, String fastModel, String embedModel) {
        this.chat = chatModel;
        this.fast = fastModel;
        this.embed = embedModel;
    }

    /**
     * The configured id if it is listed with the required method; otherwise the closest listed variant: the id with
     * {@code -preview} added or removed, then the shortest listed id that starts with the configured one.
     */
    static Optional<String> resolve(String configured, String method, List<Listed> available) {
        List<Listed> usable = available.stream().filter(m -> m.methods().contains(method)).toList();
        if (usable.stream().anyMatch(m -> m.id().equals(configured))) {
            return Optional.of(configured);
        }
        String base = configured.endsWith("-preview")
                ? configured.substring(0, configured.length() - "-preview".length()) : configured;
        for (String candidate : List.of(base + "-preview", base)) {
            if (usable.stream().anyMatch(m -> m.id().equals(candidate))) {
                return Optional.of(candidate);
            }
        }
        return usable.stream().map(Listed::id).filter(id -> id.startsWith(base + "-"))
                .min(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
    }
}
