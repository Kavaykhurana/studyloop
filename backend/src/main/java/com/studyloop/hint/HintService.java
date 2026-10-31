package com.studyloop.hint;

import com.studyloop.ai.AiFeature;
import com.studyloop.ai.AiRequest;
import com.studyloop.ai.AiResult;
import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.LlmClient;
import com.studyloop.ai.ModelTier;
import com.studyloop.ai.Prompts;
import com.studyloop.ai.Schemas;
import com.studyloop.ai.dto.HintResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.text.PlainText;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.config.AppProperties;
import com.studyloop.doubt.Doubt;
import com.studyloop.doubt.DoubtRepository;
import com.studyloop.embedding.EmbeddingStore;
import com.studyloop.embedding.SimilarSolved;
import com.studyloop.hint.dto.HintDto;
import com.studyloop.hint.dto.HintNextResponse;
import com.studyloop.subject.SubjectCatalog;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Four-rung Socratic ladder. Rungs are generated once per doubt, grounded in campus answers, and shared. */
@Service
public class HintService {

    static final int MAX_LEVEL = 4;
    static final String BUSY_MESSAGE =
            "The hint tutor is busy right now. Try again in a moment — your matched seniors have already been notified.";
    private static final Pattern CITATION = Pattern.compile("\\[#(\\d+)\\]");
    private static final int MAX_CONTENT = 4000;

    private final DoubtRepository doubts;
    private final HintDao hints;
    private final EmbeddingStore embeddings;
    private final LlmClient llm;
    private final Prompts prompts;
    private final Schemas schemas;
    private final SubjectCatalog catalog;
    private final RateLimiter limiter;
    private final AppProperties props;

    public HintService(DoubtRepository doubts, HintDao hints, EmbeddingStore embeddings, LlmClient llm,
                       Prompts prompts, Schemas schemas, SubjectCatalog catalog, RateLimiter limiter,
                       AppProperties props) {
        this.doubts = doubts;
        this.hints = hints;
        this.embeddings = embeddings;
        this.llm = llm;
        this.prompts = prompts;
        this.schemas = schemas;
        this.catalog = catalog;
        this.limiter = limiter;
        this.props = props;
    }

    public HintNextResponse next(CurrentUser me, long doubtId) {
        Doubt doubt = doubts.findByIdAndCampusId(doubtId, me.campusId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        limiter.check(RateLimitKey.HINT, String.valueOf(me.userId()));
        int unlocked = hints.levelUnlocked(me.userId(), doubtId);
        if (unlocked >= MAX_LEVEL) {
            throw new ApiException(ErrorCode.ALL_HINTS_UNLOCKED);
        }
        int target = unlocked + 1;
        boolean fromCache = true;
        Optional<HintDao.Row> row = hints.find(doubtId, target);
        if (row.isEmpty()) {
            fromCache = false;
            generate(doubt, target, me.campusId());
            row = hints.find(doubtId, target);
        }
        HintDao.Row stored = row.orElseThrow(() -> new ApiException(ErrorCode.AI_UNAVAILABLE, BUSY_MESSAGE));
        hints.upsertProgress(me.userId(), doubtId, target);
        HintDto dto = hints.toDtos(List.of(stored), me.campusId()).get(0);
        return new HintNextResponse(dto, target, fromCache);
    }

    private void generate(Doubt doubt, int level, long campusId) {
        List<SimilarSolved> grounding = grounding(doubt, campusId);
        List<HintDao.Row> previous = hints.findUpTo(doubt.getId(), level - 1);
        String subjectName = catalog.byId(doubt.getSubjectId()).orElseThrow().name();
        String topicName = catalog.topic(doubt.getTopicId()).map(t -> t.name()).orElse(SubjectCatalog.GENERAL);
        AiRequest request = new AiRequest(AiFeature.HINT, ModelTier.CHAT, Prompts.HINT_SYSTEM,
                prompts.hintUser(level, subjectName, topicName, doubt.getTitle(), doubt.getBodyMd(),
                        previousRungs(previous), knowledge(grounding)),
                null, null, schemas.hint(), 1024, Duration.ofSeconds(15));
        AiResult<HintResult> result;
        try {
            result = llm.generateJson(request, HintResult.class);
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_UNAVAILABLE, BUSY_MESSAGE);
        }
        Set<Long> allowed = new LinkedHashSet<>();
        grounding.forEach(g -> allowed.add(g.doubtId()));
        HintResult clean;
        try {
            clean = sanitize(result.value(), allowed);
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_UNAVAILABLE, BUSY_MESSAGE);
        }
        hints.insertIfAbsent(doubt.getId(), level, clean.contentMd(), clean.citedDoubtIds(), clean.confidence(),
                result.model());
    }

    private List<SimilarSolved> grounding(Doubt doubt, long campusId) {
        Optional<String> vector = embeddings.getDoubtEmbedding(doubt.getId());
        if (vector.isEmpty()) {
            return List.of();
        }
        double threshold = props.similarity().groundingThreshold();
        return embeddings.findSimilarSolved(campusId, vector.get(), 6, doubt.getId()).stream()
                .filter(s -> s.similarity() >= threshold)
                .limit(3)
                .toList();
    }

    private static String previousRungs(List<HintDao.Row> previous) {
        if (previous.isEmpty()) {
            return "(none)";
        }
        List<String> parts = new ArrayList<>();
        previous.forEach(r -> parts.add("Rung " + r.level() + ":\n" + r.contentMd()));
        return String.join("\n\n", parts);
    }

    private static String knowledge(List<SimilarSolved> grounding) {
        if (grounding.isEmpty()) {
            return "(none)";
        }
        List<String> entries = new ArrayList<>();
        for (SimilarSolved g : grounding) {
            entries.add("[#" + g.doubtId() + "] Q: " + g.title() + " — " + PlainText.clip(g.bodyMd(), 300)
                    + "\nAccepted answer: " + PlainText.clip(g.answerBody(), 700));
        }
        return String.join("\n\n", entries);
    }

    /** Drops citations of ids that were never provided and strips their inline markers. */
    static HintResult sanitize(HintResult raw, Set<Long> allowedIds) {
        String content = raw == null || raw.contentMd() == null ? "" : raw.contentMd().trim();
        if (content.isEmpty()) {
            throw new AiUnavailableException("EMPTY_HINT");
        }
        if (content.length() > MAX_CONTENT) {
            content = content.substring(0, MAX_CONTENT);
        }
        Matcher m = CITATION.matcher(content);
        StringBuilder cleaned = new StringBuilder();
        while (m.find()) {
            long id = Long.parseLong(m.group(1));
            m.appendReplacement(cleaned, allowedIds.contains(id) ? Matcher.quoteReplacement(m.group()) : "");
        }
        m.appendTail(cleaned);
        List<Long> cited = new ArrayList<>();
        if (raw.citedDoubtIds() != null) {
            raw.citedDoubtIds().stream().filter(allowedIds::contains).distinct().forEach(cited::add);
        }
        String confidence = raw.confidence() != null && Set.of("HIGH", "MEDIUM", "LOW").contains(raw.confidence())
                ? raw.confidence() : "MEDIUM";
        String text = cleaned.toString().replaceAll("[ \\t]{2,}", " ").trim();
        if (text.isEmpty()) {
            throw new AiUnavailableException("EMPTY_HINT");
        }
        return new HintResult(text, cited, confidence);
    }
}
