package com.studyloop.ai.dto;

import java.util.List;

public record HintResult(String contentMd, List<Long> citedDoubtIds, String confidence) {}
