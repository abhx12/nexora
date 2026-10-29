package com.nexora.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexora.identity.api.dto.LoginRequest;
import com.nexora.identity.api.dto.MeResponse;
import com.nexora.shared.error.ResourceNotFoundException;
import com.nexora.identity.api.dto.RefreshRequest;
import com.nexora.identity.api.dto.TokenResponse;
import com.nexora.identity.domain.RefreshToken;
import com.nexora.identity.domain.Role;
import com.nexora.identity.domain.User;
import com.nexora.identity.infrastructure.RefreshTokenRepository;
import com.nexora.identity.infrastructure.UserRepository;
import com.nexora.shared.audit.AuditAction;
import com.nexora.shared.audit.AuditService;
import com.nexora.shared.error.BusinessException;
import com.nexora.shared.error.ErrorCode;
import com.nexora.shared.security.SecurityProperties;

// Login use cases (09-SECURITY 2.1). Each public method is one transaction.
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // One message for every login failure: the caller cannot tell "no such user" from "wrong password".
    static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokens;
    private final SecurityProperties properties;
    private final Clock clock;

    // A real BCrypt hash of a random value. Unknown users are checked against it, so a failed login
    // takes the same time whether or not the username exists (no user enumeration by timing).
    private final String dummyHash;
    private final AuditService audit;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokens, SecurityProperties properties, Clock clock,
                       AuditService audit) {
        this.audit = audit;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // noRollbackFor: a wrong password throws INVALID_CREDENTIALS, but the increased failure counter
    // must still be saved. Without it the exception would roll the counter back and the lock would never trigger.
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(LoginRequest request) {
        Instant now = clock.instant();
        // Row lock: concurrent attempts for one account run one after another, so no failure is lost.
        User user = users.findByUsernameForUpdate(User.normalizeUsername(request.username())).orElse(null);

        if (user == null || !user.isActive() || user.isLocked(now)) {
            passwordEncoder.matches(request.password(), dummyHash);   // same work as a real check
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            boolean lockedNow = user.recordFailedLogin(now, properties.maxFailedLogins(), properties.lockoutDuration());
            if (lockedNow) {
                // security event; never the password
                log.warn("event=login_locked userId={} lockedUntil={}", user.getId(), user.getLockedUntil());
                audit.record(AuditAction.LOGIN_LOCKED, "USER", user.getId(), "too many failed logins",
                        Map.of("lockedUntil", user.getLockedUntil().toString()));
            }
            throw invalidCredentials();
        }
        user.recordSuccessfulLogin(now);
        return issueTokens(user, now);
    }

    // Rotation (09-SECURITY 2.3): the presented refresh token is revoked and a new pair is issued.
    // noRollbackFor: when reuse is detected, revoking all the user's tokens must be saved even though
    // the request itself fails.
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse refresh(RefreshRequest request) {
        Instant now = clock.instant();
        // Row lock: two simultaneous refreshes of the same token cannot both succeed.
        RefreshToken token = refreshTokens.findByTokenHashForUpdate(RefreshTokenHasher.hash(request.refreshToken()))
                .orElseThrow(AuthService::unauthenticated);

        if (token.isRevoked()) {
            // This token was already rotated (or logged out) and is being used again: treat it as stolen.
            // Revoke every token of the user, so the thief and the real user both have to log in again.
            refreshTokens.revokeAllActive(token.getUserId(), now);
            log.warn("event=refresh_token_reuse userId={}", token.getUserId());
            audit.record(AuditAction.REFRESH_TOKEN_REUSE, "USER", token.getUserId(), "revoked refresh token used again",
                    null);
            throw unauthenticated();
        }
        if (token.isExpired(now)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "The refresh token has expired. Please log in again.");
        }
        User user = users.findById(token.getUserId()).orElseThrow(AuthService::unauthenticated);
        if (!user.isActive()) {
            throw unauthenticated();
        }
        token.revoke(now);                          // the old token can never be used again
        return issueTokens(user, now);
    }

    // Logout revokes the presented refresh token. Unknown or already revoked tokens are fine too:
    // logout is idempotent, and the response never reveals whether a token existed.
    @Transactional
    public void logout(RefreshRequest request) {
        Instant now = clock.instant();
        refreshTokens.findByTokenHashForUpdate(RefreshTokenHasher.hash(request.refreshToken()))
                .ifPresent(token -> token.revoke(now));
    }

    // The caller's own profile, roles and permissions (read from the database, so it reflects current roles).
    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return new MeResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.permissions().stream().map(Enum::name).sorted().toList(),
                user.getLastLoginAt());
    }

    @Transactional
    public void changePassword(UUID userId, com.nexora.identity.api.dto.ChangePasswordRequest request) {
        Instant now = clock.instant();
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password does not match.");
        }

        String newPassword = request.newPassword();
        int bytes = newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        boolean hasLetter = newPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        if (bytes < 12 || bytes > 72 || !hasLetter || !hasDigit || newPassword.equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException(ErrorCode.MALFORMED_REQUEST,
                    "New password must be 12-72 characters long, contain at least one letter and one number, and not match the username");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.MALFORMED_REQUEST, "New password cannot be the same as the current password");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokens.revokeAllActive(userId, now);
        log.info("event=password_changed userId={}", userId);
    }

    // A new access token plus a new refresh token (stored as a hash only).
    private TokenResponse issueTokens(User user, Instant now) {
        String accessToken = accessTokens.issue(user.getId(), user.getUsername(),
                user.getRoles().stream().map(Role::getName).toList(), user.permissions());
        String refreshToken = RefreshTokenHasher.newToken();
        refreshTokens.save(RefreshToken.issue(user.getId(), RefreshTokenHasher.hash(refreshToken),
                now.plus(properties.refreshTokenTtl())));
        return new TokenResponse(accessToken, refreshToken, "Bearer", accessTokens.accessTokenSeconds());
    }

    private static BusinessException unauthenticated() {
        return new BusinessException(ErrorCode.UNAUTHENTICATED, "The refresh token is not valid. Please log in again.");
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS_MESSAGE);
    }
}
