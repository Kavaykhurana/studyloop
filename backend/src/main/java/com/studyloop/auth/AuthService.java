package com.studyloop.auth;

import com.studyloop.auth.dto.AuthResponse;
import com.studyloop.auth.dto.LoginRequest;
import com.studyloop.auth.dto.SignupRequest;
import com.studyloop.campus.CampusResolver;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.config.AppProperties;
import com.studyloop.user.User;
import com.studyloop.user.UserRepository;
import com.studyloop.user.UserService;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public record Session(AuthResponse body, String refreshToken) {}

    public enum Persona {
        LEARNER("aanya.sharma@demo.studyloop.app"),
        HELPER("rohan.mehta@demo.studyloop.app");

        private final String email;

        Persona(String email) {
            this.email = email;
        }

        public String email() {
            return email;
        }

        public static Persona parse(String value) {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    private static final Pattern HAS_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    private final UserRepository users;
    private final CampusResolver campusResolver;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final UserService userService;
    private final AppProperties props;
    private final Clock clock;
    private volatile String dummyHash;

    public AuthService(UserRepository users, CampusResolver campusResolver, PasswordEncoder passwordEncoder,
                       TokenService tokens, UserService userService, AppProperties props, Clock clock) {
        this.users = users;
        this.campusResolver = campusResolver;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.userService = userService;
        this.props = props;
        this.clock = clock;
    }

    public Session signup(SignupRequest req, String userAgent) {
        String password = req.password();
        if (password.length() < 8 || !HAS_LETTER.matcher(password).find() || !HAS_DIGIT.matcher(password).find()) {
            throw new ApiException(ErrorCode.WEAK_PASSWORD);
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        User user = new User();
        user.setCampusId(campusResolver.resolve(email));
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setYearOfStudy(req.yearOfStudy());
        user.setBranch(req.branch() == null || req.branch().isBlank() ? null : req.branch().trim());
        user.setCreatedAt(clock.instant());
        user.setLastActiveAt(clock.instant());
        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        return session(user, userAgent);
    }

    public Session login(LoginRequest req, String userAgent) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);
        String hash = user == null || user.getPasswordHash() == null ? dummy() : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(req.password(), hash);
        if (user == null || user.getPasswordHash() == null || !matches) {
            throw new ApiException(ErrorCode.BAD_CREDENTIALS);
        }
        return session(user, userAgent);
    }

    public Session demo(Persona persona, String userAgent) {
        if (!props.demo().enabled()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        User user = users.findByEmail(persona.email())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Demo users are not available yet."));
        return session(user, userAgent);
    }

    public Session refresh(String rawToken, String userAgent) {
        TokenService.Rotation rotation = tokens.rotate(rawToken, userAgent);
        return new Session(response(rotation.user()), rotation.refreshToken());
    }

    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            tokens.revokeFamilyOf(rawToken);
        }
    }

    private Session session(User user, String userAgent) {
        String refresh = tokens.startSession(user, userAgent);
        return new Session(response(user), refresh);
    }

    private AuthResponse response(User user) {
        TokenService.AccessToken access = tokens.issueAccessToken(user);
        return new AuthResponse(access.token(), access.expiresInSeconds(), userService.toMe(user));
    }

    private String dummy() {
        String h = dummyHash;
        if (h == null) {
            h = passwordEncoder.encode("studyloop-dummy-password");
            dummyHash = h;
        }
        return h;
    }
}
