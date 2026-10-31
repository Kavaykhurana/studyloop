package com.studyloop.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes the RFC 9457 error body outside of Spring MVC (security filters). */
@Component
public class ErrorResponseWriter {

    private final JsonMapper jsonMapper;

    public ErrorResponseWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", code.typeUri());
        body.put("title", code.title());
        body.put("status", code.httpStatus().value());
        body.put("detail", code.defaultDetail());
        body.put("instance", request.getRequestURI());
        body.put("code", code.name());
        body.put("traceId", MDC.get(TraceIdFilter.MDC_KEY));
        response.setStatus(code.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
