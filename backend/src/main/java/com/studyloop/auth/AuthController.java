package com.studyloop.auth;

import com.studyloop.auth.dto.AuthResponse;
import com.studyloop.auth.dto.DemoRequest;
import com.studyloop.auth.dto.LoginRequest;
import com.studyloop.auth.dto.SignupRequest;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.ratelimit.RateLimitKey;
import com.studyloop.common.ratelimit.RateLimiter;
import com.studyloop.common.web.ClientIp;
import com.studyloop.config.AppProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

    static final String COOKIE = "sl_refresh";
    private static final String CSRF_HEADER = "X-Requested-With";
    private static final String CSRF_VALUE = "StudyLoop";

    private final AuthService auth;
    private final RateLimiter limiter;
    private final AppProperties props;

    public AuthController(AuthService auth, RateLimiter limiter, AppProperties props) {
        this.auth = auth;
        this.limiter = limiter;
        this.props = props;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
        limiter.check(RateLimitKey.SIGNUP, ClientIp.of(http));
        return respond(HttpStatus.CREATED, auth.signup(req, http.getHeader(HttpHeaders.USER_AGENT)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        limiter.check(RateLimitKey.LOGIN, ClientIp.of(http));
        return respond(HttpStatus.OK, auth.login(req, http.getHeader(HttpHeaders.USER_AGENT)));
    }

    @PostMapping("/demo")
    public ResponseEntity<AuthResponse> demo(@Valid @RequestBody DemoRequest req, HttpServletRequest http) {
        limiter.check(RateLimitKey.DEMO, ClientIp.of(http));
        return respond(HttpStatus.OK,
                auth.demo(AuthService.Persona.parse(req.persona()), http.getHeader(HttpHeaders.USER_AGENT)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(value = CSRF_HEADER, required = false) String requestedWith,
            @CookieValue(value = COOKIE, required = false) String cookie, HttpServletRequest http) {
        requireHeader(requestedWith);
        if (cookie == null || cookie.isBlank()) {
            throw new ApiException(ErrorCode.REFRESH_INVALID).withHeader(HttpHeaders.SET_COOKIE, cleared().toString());
        }
        try {
            return respond(HttpStatus.OK, auth.refresh(cookie, http.getHeader(HttpHeaders.USER_AGENT)));
        } catch (ApiException e) {
            if (e.code() == ErrorCode.REFRESH_INVALID || e.code() == ErrorCode.REFRESH_REUSED) {
                e.withHeader(HttpHeaders.SET_COOKIE, cleared().toString());
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = CSRF_HEADER, required = false) String requestedWith,
                                       @CookieValue(value = COOKIE, required = false) String cookie) {
        requireHeader(requestedWith);
        auth.logout(cookie);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared().toString()).build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthService.Session session) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .body(session.body());
    }

    private static void requireHeader(String value) {
        if (!CSRF_VALUE.equals(value)) {
            throw new ApiException(ErrorCode.CSRF_HEADER_MISSING);
        }
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(COOKIE, token)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(props.jwt().refreshTtlDays()))
                .build();
    }

    private ResponseCookie cleared() {
        return ResponseCookie.from(COOKIE, "")
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
