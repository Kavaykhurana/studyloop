package com.studyloop.ai.dto;

public record SnapResult(Boolean readable, String title, String bodyMd, String subjectCode, String topic,
                         String confidence) {}
