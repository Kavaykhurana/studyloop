package com.studyloop.ai;

import com.studyloop.config.AppProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Thin REST transport for the Gemini API: one shared HttpClient, a per-call read timeout. */
@Component
public class GeminiTransport {

    private final HttpClient httpClient;
    private final AppProperties.Ai ai;

    public GeminiTransport(HttpClient geminiHttpClient, AppProperties props) {
        this.httpClient = geminiHttpClient;
        this.ai = props.ai();
    }

    /** POSTs a JSON body to {baseUrl}{path}; throws RestClientResponseException / ResourceAccessException. */
    public String post(String path, String jsonBody, Duration timeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(ai.baseUrl())
                .build()
                .post()
                .uri(path)
                .header("x-goog-api-key", ai.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .retrieve()
                .body(String.class);
    }

    public String get(String path, Duration timeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(ai.baseUrl())
                .build()
                .get()
                .uri(path)
                .header("x-goog-api-key", ai.apiKey())
                .retrieve()
                .body(String.class);
    }
}
