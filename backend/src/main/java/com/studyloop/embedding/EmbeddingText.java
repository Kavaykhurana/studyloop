package com.studyloop.embedding;

public final class EmbeddingText {

    private static final int MAX_BODY = 1500;

    private EmbeddingText() {}

    public static String forDoubt(String title, String subjectName, String topicName, String body) {
        return "Title: " + title + "\nSubject: " + subjectName + " / " + topicName + "\n\n" + truncate(body);
    }

    public static String forQuery(String title, String body) {
        return "Title: " + title + "\n\n" + truncate(body);
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= MAX_BODY ? body : body.substring(0, MAX_BODY);
    }
}
