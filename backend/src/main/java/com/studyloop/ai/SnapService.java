package com.studyloop.ai;

import com.studyloop.ai.dto.SnapResponse;
import com.studyloop.ai.dto.SnapResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.subject.dto.TopicRef;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Turns a photo into an editable doubt. The image lives only in memory for the duration of the call. */
@Service
public class SnapService {

    private static final long MAX_BYTES = 4L * 1024 * 1024;

    private final LlmClient llm;
    private final Prompts prompts;
    private final Schemas schemas;
    private final SubjectCatalog catalog;
    private final RateLimiter limiter;

    public SnapService(LlmClient llm, Prompts prompts, Schemas schemas, SubjectCatalog catalog, RateLimiter limiter) {
        this.llm = llm;
        this.prompts = prompts;
        this.schemas = schemas;
        this.catalog = catalog;
        this.limiter = limiter;
    }

    public SnapResponse snap(CurrentUser me, MultipartFile image) {
        limiter.check(RateLimitKey.SNAP, String.valueOf(me.userId()));
        if (image == null || image.isEmpty()) {
            throw new ApiException(ErrorCode.UNSUPPORTED_IMAGE);
        }
        if (image.getSize() > MAX_BYTES) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.UNSUPPORTED_IMAGE);
        }
        String mime = detectMime(bytes);
        if (mime == null) {
            throw new ApiException(ErrorCode.UNSUPPORTED_IMAGE);
        }
        AiRequest request = new AiRequest(AiFeature.SNAP, ModelTier.CHAT, Prompts.SNAP_SYSTEM,
                prompts.snapUser(catalog.topicNamesByCode()), bytes, mime, schemas.snap(), 1024,
                Duration.ofSeconds(20));
        SnapResult result;
        try {
            result = llm.generateJson(request, SnapResult.class).value();
        } catch (AiUnavailableException e) {
            throw new ApiException(ErrorCode.AI_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(result.readable()) || isBlank(result.title()) || isBlank(result.bodyMd())) {
            throw new ApiException(ErrorCode.IMAGE_UNREADABLE);
        }
        Optional<SubjectInfo> subject = catalog.byCode(result.subjectCode());
        TopicRef topic = subject.map(s -> catalog.topicByName(s.id(), result.topic())
                .orElseGet(() -> catalog.general(s.id()))).orElse(null);
        String title = clip(result.title().trim(), 160);
        return new SnapResponse(title, clip(result.bodyMd().trim(), 6000), subject.map(SubjectInfo::id).orElse(null),
                topic == null ? null : topic.id(), result.confidence() == null ? "MEDIUM" : result.confidence());
    }

    /** Checks the file signature, not the client-supplied content type. */
    static String detectMime(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D
                && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E'
                && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
