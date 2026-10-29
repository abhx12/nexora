package com.nexora.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.nexora.shared.domain.AuditableEntity;
import com.nexora.shared.security.Permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

// Someone who logs in to the application. A user is NOT a party (customers, suppliers ... are parties).
// The login lock state lives here so it can be checked and changed under a row lock (09-SECURITY 2.1).
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Column(nullable = false, length = 100, unique = true, updatable = false)
    private String username;                       // always lower case (DB CHECK enforces it too)

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;                   // "{bcrypt}$2a$10$..."; the plain password is never stored

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;               // consecutive wrong passwords since the last success

    @Column(name = "locked_until")
    private Instant lockedUntil;                   // null = not locked

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // user_roles join table. No cascade: roles are shared master data and are never created through a user.
    @ManyToMany
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {
        // for JPA only
    }

    // Factory: the only way to create a user. The caller passes an already hashed password.
    public static User create(String username, String passwordHash, String displayName, Role role) {
        User user = new User();
        user.username = normalizeUsername(username);
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.active = true;
        user.roles.add(role);
        return user;
    }

    // Usernames are case-insensitive: "Owner " and "owner" are the same account.
    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    // True while a lock is in force. A lock whose time has passed no longer counts.
    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    // A wrong password. Returns true when this failure locked the account (so the caller can log it).
    public boolean recordFailedLogin(Instant now, int maxFailedLogins, Duration lockoutDuration) {
        if (lockedUntil != null && !isLocked(now)) {
            // the previous lock has expired: start counting again from zero
            lockedUntil = null;
            failedLoginAttempts = 0;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxFailedLogins && lockedUntil == null) {
            lockedUntil = now.plus(lockoutDuration);
            return true;
        }
        return false;
    }

    // Correct password: the counter and any expired lock are cleared.
    public void recordSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    // Every permission of every role of this user (roles are just bundles of permissions).
    public Set<Permission> permissions() {
        Set<Permission> all = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> all.addAll(role.getPermissions()));
        return all;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
