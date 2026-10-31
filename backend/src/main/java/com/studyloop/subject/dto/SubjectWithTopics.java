package com.studyloop.subject.dto;

import java.util.List;

public record SubjectWithTopics(Long id, String code, String name, String colorHex, List<TopicRef> topics) {}
