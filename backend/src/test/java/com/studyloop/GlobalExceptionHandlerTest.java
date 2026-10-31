package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyloop.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class GlobalExceptionHandlerTest extends IntegrationTestBase {

    @Test
    void validationErrorsFollowTheProblemDetailShape() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"email\":\"not-an-email\",\"password\":\"p\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://studyloop.app/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("2 fields are invalid."))
                .andExpect(jsonPath("$.instance").value("/api/auth/signup"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        assertThat(result.getResponse().getHeader("X-Trace-Id")).hasSize(8);
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.detail").value("Request body is not valid JSON."));
    }

    @Test
    void wrongContentTypeIsUnsupportedMediaTypeNotAServerError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_FORM_URLENCODED).content("a=b"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void scriptTagsInDoubtsAreRejectedAsUnsafe() throws Exception {
        var user = fx.tiet("Asha Learner");
        mvc.perform(post("/api/doubts").header("Authorization", fx.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A title that has enough characters\",\"bodyMd\":\"<SCRIPT>alert(1)</SCRIPT> "
                                + "and enough text to pass the length rule\",\"subjectId\":1,\"source\":\"TEXT\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNSAFE_CONTENT"));
    }
}
