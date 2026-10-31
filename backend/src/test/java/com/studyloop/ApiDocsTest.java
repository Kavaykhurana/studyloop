package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.studyloop.support.IntegrationTestBase;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Judges open /api/docs without signing in, so the whole Swagger UI chain must be public. */
class ApiDocsTest extends IntegrationTestBase {

    @Test
    void swaggerUiAndTheOpenApiDocumentArePublic() throws Exception {
        MvcResult docs = mvc.perform(get("/api/docs")).andReturn();
        assertThat(docs.getResponse().getStatus()).isEqualTo(302);
        String ui = docs.getResponse().getRedirectedUrl();
        assertThat(ui).isNotNull();

        // The UI files come from the swagger-ui webjar; what matters here is that security lets them through.
        assertThat(mvc.perform(get(URI.create(ui).getPath())).andReturn().getResponse().getStatus())
                .isNotIn(401, 403);
        assertThat(mvc.perform(get("/api/docs/openapi.json")).andReturn().getResponse().getContentAsString())
                .contains("StudyLoop API");
        assertThat(mvc.perform(get("/api/docs/openapi.json/swagger-config")).andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}
