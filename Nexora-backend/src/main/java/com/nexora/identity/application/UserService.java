package com.nexora.identity.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexora.identity.api.dto.CreateUserRequest;
import com.nexora.identity.api.dto.ToggleUserStatusRequest;
import com.nexora.identity.api.dto.UserResponse;
import com.nexora.identity.domain.Role;
import com.nexora.identity.domain.User;
import com.nexora.identity.infrastructure.RefreshTokenRepository;
import com.nexora.identity.infrastructure.RoleRepository;
import com.nexora.identity.infrastructure.UserRepository;
import com.nexora.shared.audit.AuditAction;
import com.nexora.shared.audit.AuditService;
import com.nexora.shared.error.BusinessException;
import com.nexora.shared.error.ErrorCode;
import com.nexora.shared.error.ResourceNotFoundException;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AuditService audit;

    public UserService(UserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, Clock clock, AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        String normalized = User.normalizeUsername(request.username());
        if (users.findByUsername(normalized).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Username '" + request.username() + "' is already taken");
        }

        String password = request.password();
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (bytes < 12 || bytes > 72 || !hasLetter || !hasDigit || password.equalsIgnoreCase(normalized)) {
            throw new BusinessException(ErrorCode.MALFORMED_REQUEST,
                    "Password must be 12-72 characters long, contain at least one letter and one number, and not match the username");
        }

        Role role = roles.findByName(request.role().trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.MALFORMED_REQUEST, "Role '" + request.role() + "' does not exist"));

        User user = User.create(normalized, passwordEncoder.encode(password), request.displayName().trim(), role);
        user = users.save(user);

        log.info("event=user_created username={} role={}", normalized, role.getName());
        audit.record(AuditAction.USER_CREATED, "USER", user.getId(), "User account created",
                Map.of("username", normalized, "role", role.getName()));

        return toResponse(user);
    }

    public UserResponse setUserStatus(UUID targetUserId, ToggleUserStatusRequest request, UUID callerId) {
        Instant now = clock.instant();
        User target = users.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (target.getId().equals(callerId) && !request.active()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "You cannot deactivate your own account");
        }

        boolean wasActive = target.isActive();
        target.setActive(request.active());

        if (wasActive && !request.active()) {
            refreshTokens.revokeAllActive(targetUserId, now);
            audit.record(AuditAction.USER_DEACTIVATED, "USER", targetUserId, "User account deactivated", null);
        }

        log.info("event=user_status_changed userId={} active={}", targetUserId, request.active());
        return toResponse(target);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isActive(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
