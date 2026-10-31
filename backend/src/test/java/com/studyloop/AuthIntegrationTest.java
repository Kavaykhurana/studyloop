package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyloop.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "pass1234";

    private MvcResult signup(String email) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Student\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn();
    }

    private MvcResult refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/auth/refresh").header("X-Requested-With", "StudyLoop").cookie(cookie))
                .andReturn();
    }

    @Test
    void signupReturnsCreatedWithTokenAndHttpOnlyCookie() throws Exception {
        MvcResult result = signup("a@thapar.edu");
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(result.getResponse().getContentAsString()).contains("accessToken").contains("TIET");
        Cookie cookie = result.getResponse().getCookie("sl_refresh");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("SameSite=Lax");
    }

    @Test
    void campusIsResolvedFromEmailDomainIncludingSubdomains() throws Exception {
        assertThat(signup("a@thapar.edu").getResponse().getContentAsString()).contains("\"shortName\":\"TIET\"");
        assertThat(signup("b@stu.thapar.edu").getResponse().getContentAsString()).contains("\"shortName\":\"TIET\"");
        assertThat(signup("c@gmail.com").getResponse().getContentAsString()).contains("\"shortName\":\"OPEN\"");
    }

    @Test
    void duplicateEmailIsRejectedWithConflict() throws Exception {
        signup("dup@thapar.edu");
        MvcResult second = signup("DUP@thapar.edu");
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
        assertThat(second.getResponse().getContentAsString()).contains("EMAIL_TAKEN");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() throws Exception {
        signup("known@thapar.edu");
        MvcResult wrong = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"known@thapar.edu\",\"password\":\"wrongpass1\"}")).andReturn();
        MvcResult unknown = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@thapar.edu\",\"password\":\"wrongpass1\"}")).andReturn();
        assertThat(wrong.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        String message = "Email or password is incorrect.";
        assertThat(wrong.getResponse().getContentAsString()).contains(message).contains("BAD_CREDENTIALS");
        assertThat(unknown.getResponse().getContentAsString()).contains(message).contains("BAD_CREDENTIALS");
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Student\",\"email\":\"w@thapar.edu\",\"password\":\"onlyletters\"}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(result.getResponse().getContentAsString()).contains("WEAK_PASSWORD");
    }

    @Test
    void refreshRotatesAndReusingAnOldTokenRevokesTheWholeFamily() throws Exception {
        Cookie first = signup("rot@thapar.edu").getResponse().getCookie("sl_refresh");

        MvcResult rotated = refresh(first);
        assertThat(rotated.getResponse().getStatus()).isEqualTo(200);
        Cookie second = rotated.getResponse().getCookie("sl_refresh");
        assertThat(second).isNotNull();
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        MvcResult reuse = refresh(first);
        assertThat(reuse.getResponse().getStatus()).isEqualTo(401);
        assertThat(reuse.getResponse().getContentAsString()).contains("REFRESH_REUSED");

        MvcResult afterReuse = refresh(second);
        assertThat(afterReuse.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void refreshWithoutTheCsrfHeaderIsForbidden() throws Exception {
        Cookie cookie = signup("csrf@thapar.edu").getResponse().getCookie("sl_refresh");
        mvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_HEADER_MISSING"));
    }

    @Test
    void logoutClearsTheCookieAndRevokesTheSession() throws Exception {
        Cookie cookie = signup("out@thapar.edu").getResponse().getCookie("sl_refresh");
        MvcResult logout = mvc.perform(post("/api/auth/logout").header("X-Requested-With", "StudyLoop")
                .cookie(cookie)).andReturn();
        assertThat(logout.getResponse().getStatus()).isEqualTo(204);
        assertThat(logout.getResponse().getCookie("sl_refresh").getMaxAge()).isZero();
        assertThat(refresh(cookie).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void protectedEndpointWithoutTokenReturnsJsonErrorBody() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void demoLoginIsAvailableWhenDemoModeIsOn() throws Exception {
        fx.user("Aanya Sharma", 1, 2);
        jdbc.update("UPDATE users SET email = 'aanya.sharma@demo.studyloop.app' WHERE name = 'Aanya Sharma'");
        mvc.perform(post("/api/auth/demo").contentType(MediaType.APPLICATION_JSON).content("{\"persona\":\"learner\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.isDemo").value(true));
    }
}
