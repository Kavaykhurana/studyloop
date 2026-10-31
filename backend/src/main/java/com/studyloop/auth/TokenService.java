package com.studyloop.auth;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.config.AppProperties;
import com.studyloop.config.JwtConfig;
import com.studyloop.user.User;
import com.studyloop.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    public record AccessToken(String token, long expiresInSeconds) {}

    public record Rotation(String refreshToken, User user) {}

    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final AppProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens, UserRepository users,
                        AppProperties props, Clock clock) {
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.props = props;
        this.clock = clock;
    }

    public AccessToken issueAccessToken(User user) {
        Instant now = clock.instant();
        Duration ttl = Duration.ofMinutes(props.jwt().accessTtlMinutes());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("cmp", user.getCampusId())
                .claim("role", user.getRole())
                .claim("demo", user.isDemo())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AccessToken(token, ttl.toSeconds());
    }

    /** Starts a new token family (one per login) and returns the opaque cookie value. */
    @Transactional
    public String startSession(User user, String userAgent) {
        return issueRefresh(user.getId(), UUID.randomUUID(), userAgent).raw();
    }

    /**
     * Rotates a refresh token. A token that was already used revokes its whole family (reuse detection).
     * The revoke must commit even though we throw, hence noRollbackFor.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String rawToken, String userAgent) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByHashForUpdate(sha256Hex(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_INVALID));
        if (current.getRevokedAt() != null) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new ApiException(ErrorCode.REFRESH_REUSED);
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.REFRESH_INVALID);
        }
        User user = users.findById(current.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_INVALID));
        current.setRevokedAt(now);
        Issued next = issueRefresh(user.getId(), current.getFamilyId(), userAgent);
        current.setReplacedById(next.entity().getId());
        refreshTokens.save(current);
        return new Rotation(next.raw(), user);
    }

    @Transactional
    public void revokeFamilyOf(String rawToken) {
        refreshTokens.findByTokenHash(sha256Hex(rawToken))
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    private record Issued(String raw, RefreshToken entity) {}

    private Issued issueRefresh(Long userId, UUID family, String userAgent) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        RefreshToken t = new RefreshToken();
        t.setUserId(userId);
        t.setTokenHash(sha256Hex(raw));
        t.setFamilyId(family);
        t.setCreatedAt(now);
        t.setExpiresAt(now.plus(Duration.ofDays(props.jwt().refreshTtlDays())));
        t.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(200, userAgent.length())));
        return new Issued(raw, refreshTokens.save(t));
    }

    static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
