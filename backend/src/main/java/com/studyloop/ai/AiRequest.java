package com.studyloop.ai;

import java.time.Duration;
import java.util.Map;

public record AiRequest(AiFeature feature, ModelTier tier, String systemPrompt, String userText,
                        byte[] imageBytes, String imageMimeType, Map<String, Object> responseJsonSchema,
                        int maxOutputTokens, Duration timeout) {}
