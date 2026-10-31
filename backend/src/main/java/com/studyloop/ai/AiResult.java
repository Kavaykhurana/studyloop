package com.studyloop.ai;

public record AiResult<T>(T value, String model, long latencyMs) {}
