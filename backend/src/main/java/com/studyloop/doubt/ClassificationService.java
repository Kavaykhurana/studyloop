package com.studyloop.doubt;

import com.studyloop.ai.AiFeature;
import com.studyloop.ai.AiRequest;
import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.LlmClient;
import com.studyloop.ai.ModelTier;
import com.studyloop.ai.Prompts;
import com.studyloop.ai.Schemas;
import com.studyloop.ai.dto.ClassifyResult;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);

    private final LlmClient llm;
    private final Prompts prompts;
    private final Schemas schemas;
    private final SubjectCatalog catalog;

    public ClassificationService(LlmClient llm, Prompts prompts, Schemas schemas, SubjectCatalog catalog) {
        this.llm = llm;
        this.prompts = prompts;
        this.schemas = schemas;
        this.catalog = catalog;
    }

    /** Empty when the AI is unavailable: classification fails open. */
    public Optional<ClassifyResult> classify(String title, String body) {
        AiRequest request = new AiRequest(AiFeature.CLASSIFY, ModelTier.FAST, Prompts.CLASSIFY_SYSTEM,
                prompts.classifyUser(catalog.topicNamesByCode(), title, body), null, null,
                schemas.classify(catalog.all().stream().map(SubjectInfo::code).toList()), 256,
                Duration.ofSeconds(6));
        try {
            return Optional.of(llm.generateJson(request, ClassifyResult.class).value());
        } catch (AiUnavailableException e) {
            log.warn("Classification skipped: {}", e.getCode());
            return Optional.empty();
        }
    }
}
