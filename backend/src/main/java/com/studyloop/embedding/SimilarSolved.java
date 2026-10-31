package com.studyloop.embedding;

import java.time.Instant;

/** A solved doubt on the same campus together with its accepted answer, ranked by cosine similarity. */
public record SimilarSolved(long doubtId, String title, String bodyMd, long subjectId, Long topicId, Instant solvedAt,
                            double similarity, long answerId, String answerBody, String answerAuthor) {}
