package com.studyloop.ai;

public interface LlmClient {

    /** Calls the model with a JSON schema and deserialises the answer; throws AiUnavailableException on failure. */
    <T> AiResult<T> generateJson(AiRequest request, Class<T> type);
}
