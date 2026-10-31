package com.studyloop.hint.dto;

public record HintNextResponse(HintDto hint, int levelUnlocked, boolean fromCache) {}
