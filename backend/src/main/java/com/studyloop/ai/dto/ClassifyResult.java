package com.studyloop.ai.dto;

public record ClassifyResult(String subjectCode, String topic, Integer difficulty, Boolean isAcademic,
                             String suggestedTitle) {}
