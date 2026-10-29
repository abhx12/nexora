package com.nexora.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.nexora.shared.security.Permission;
import com.nexora.support.AbstractApiTest;
import com.nexora.support.AuthTokens;

class AuthApiTest extends AbstractApiTest {

    @Test
    @DisplayName("FR-AUTH-01 login with valid credentials returns token pair")
    void login_withValidCredentials_returnsTokenPair() throws Exception {
        String username = tokens.createUser("OWNER");

        // usernames are case-insensitive and trimmed
        login(" " + username.toUpperCase() + " ", AuthTokens.TEST_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.hasLength(43)))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("FR-AUTH-01 NFR-TEST-04 login with wrong password returns401 invalid credentials")
    void login_withWrongPassword_returns401InvalidCredentials() throws Exception {
        String username = tokens.createUser("OWNER");

        login(username, "Wrong-Password-1")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        // the failed attempt was counted and survived the rejected request
        assertThat(failedAttempts(username)).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-AUTH-01 login with unknown user returns same message as wrong password")
    void login_withUnknownUser_returnsSameMessageAsWrongPassword() throws Exception {
        String username = tokens.createUser("OWNER");
        String wrongPassword = detail(login(username, "Wrong-Password-1"));
        String unknownUser = detail(login("nobody-" + username, "Wrong-Password-1"));

        assertThat(unknownUser).isEqualTo(wrongPassword);
    }

    @Test
    @DisplayName("FR-AUTH-01 login inactive user returns401 invalid credentials")
    void login_inactiveUser_returns401InvalidCredentials() throws Exception {
        String username = tokens.createUser("OWNER");
        // no endpoint deactivates users in the first release, so the state is set directly
        jdbc.update("update users set is_active = false where username = ?", username);

        login(username, AuthTokens.TEST_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("FR-AUTH-08 login response never contains password hash")
    void login_responseNeverContainsPasswordHash() throws Exception {
        String username = tokens.createUser("OWNER");
        String hash = jdbc.queryForObject("select password_hash from users where username = ?", String.class, username);

        String body = login(username, AuthTokens.TEST_PASSWORD).andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(hash).doesNotContain(AuthTokens.TEST_PASSWORD).doesNotContain("bcrypt");
    }

    @Test
    @DisplayName("FR-AUTH-01 login with blank password returns400 validation failed")
    void login_withBlankPassword_returns400() throws Exception {
        login("owner", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("FR-AUTH-07 login after five failures rejects correct password for15 minutes")
    void login_afterFiveFailures_rejectsCorrectPasswordFor15Minutes() throws Exception {
        String username = tokens.createUser("OWNER");
        for (int i = 0; i < 5; i++) {
            login(username, "Wrong-Password-1").andExpect(status().isUnauthorized());
        }

        // AC1: the correct password is now rejected with the same generic error
        login(username, AuthTokens.TEST_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        // the lock lasts 15 minutes from the fifth failure (checked in the database, within a minute of tolerance)
        assertThat(jdbc.queryForObject(
                "select locked_until between now() + interval '14 minutes' and now() + interval '15 minutes'"
                        + " from users where username = ?", Boolean.class, username)).isTrue();

        // AC2: once the lock time has passed (simulated by moving it into the past), the login succeeds
        jdbc.update("update users set locked_until = now() - interval '1 second' where username = ?", username);
        login(username, AuthTokens.TEST_PASSWORD).andExpect(status().isOk());
        assertThat(failedAttempts(username)).isZero();
    }

    @Test
    @DisplayName("FR-AUTH-04 refresh with valid token rotates and old token stops working")
    void refresh_withValidToken_rotatesAndOldTokenStopsWorking() throws Exception {
        String r1 = tokens.login(tokens.createUser("OWNER"), AuthTokens.TEST_PASSWORD).get("refreshToken").asString();

        String body = refresh(r1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        String r2 = jsonMapper.readTree(body).get("refreshToken").asString();

        assertThat(r2).isNotEqualTo(r1);
        assertThat(revokedAt(r1)).isNotNull();          // R1 revoked in the same transaction
        assertThat(revokedAt(r2)).isNull();
    }

    @Test
    @DisplayName("FR-AUTH-04 refresh with expired token returns401 token expired")
    void refresh_withExpiredToken_returns401TokenExpired() throws Exception {
        String r1 = tokens.login(tokens.createUser("OWNER"), AuthTokens.TEST_PASSWORD).get("refreshToken").asString();
        jdbc.update("update refresh_tokens set expires_at = now() - interval '1 second' where token_hash = ?",
                com.nexora.identity.application.RefreshTokenHasher.hash(r1));

        refresh(r1)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("FR-AUTH-04 refresh with unknown token returns401 unauthenticated")
    void refresh_withUnknownToken_returns401Unauthenticated() throws Exception {
        refresh("this-token-was-never-issued")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("FR-AUTH-04 refresh after user deactivated returns401 unauthenticated")
    void refresh_afterUserDeactivated_returns401Unauthenticated() throws Exception {
        String username = tokens.createUser("OWNER");
        String r1 = tokens.login(username, AuthTokens.TEST_PASSWORD).get("refreshToken").asString();
        jdbc.update("update users set is_active = false where username = ?", username);

        refresh(r1)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("FR-AUTH-05 refresh with rotated token revokes all tokens of user")
    void refresh_withRotatedToken_revokesAllTokensOfUser() throws Exception {
        String r1 = tokens.login(tokens.createUser("OWNER"), AuthTokens.TEST_PASSWORD).get("refreshToken").asString();
        String r2 = jsonMapper.readTree(refresh(r1).andReturn().getResponse().getContentAsString())
                .get("refreshToken").asString();

        // AC1: R1 presented again (reuse) is rejected
        refresh(r1).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        // AC2: the reuse also revoked R2
        refresh(r2).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertThat(revokedAt(r2)).isNotNull();
    }

    @Test
    @DisplayName("FR-AUTH-06 logout revokes token and is idempotent")
    void logout_revokesToken_andIsIdempotent() throws Exception {
        String r1 = tokens.login(tokens.createUser("OWNER"), AuthTokens.TEST_PASSWORD).get("refreshToken").asString();

        logout(r1).andExpect(status().isNoContent());
        logout(r1).andExpect(status().isNoContent());                  // AC2: already revoked -> still 204
        logout("never-issued-token").andExpect(status().isNoContent()); // AC2: unknown -> 204

        // AC3: a logged-out token cannot be refreshed
        refresh(r1).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("FR-AUTH-11 me returns caller roles and permissions")
    void me_returnsCallerRolesAndPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.roles[0]").value("OWNER"))
                .andExpect(jsonPath("$.permissions.length()").value(Permission.values().length))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("STAFF"))
                .andExpect(jsonPath("$.permissions.length()").value(4));
        // AC2: no token -> 401
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("FR-AUTH-14 user management allows owner to list and create users")
    void userManagement_allowsOwnerToListAndCreateUsers() throws Exception {
        String owner = AuthTokens.bearer(tokens.owner());
        // List users
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Create new user
        String newUserJson = """
                {
                    "username": "newstaff2026",
                    "displayName": "Staff Member",
                    "password": "Password12345!",
                    "role": "STAFF"
                }
                """;
        mockMvc.perform(post("/api/v1/users").header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content(newUserJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newstaff2026"))
                .andExpect(jsonPath("$.displayName").value("Staff Member"))
                .andExpect(jsonPath("$.roles[0]").value("STAFF"));
    }

    @Test
    @DisplayName("FR-AUTH-15 change password allows authenticated user to update password")
    void changePassword_allowsAuthenticatedUserToUpdatePassword() throws Exception {
        String username = tokens.createUser("OWNER");
        String token = tokens.login(username, AuthTokens.TEST_PASSWORD).get("accessToken").asString();

        String body = """
                {
                    "currentPassword": "%s",
                    "newPassword": "BrandNewSecretPassword123!"
                }
                """.formatted(AuthTokens.TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/password").header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        // Login with old password fails
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", AuthTokens.TEST_PASSWORD))))
                .andExpect(status().isUnauthorized());

        // Login with new password succeeds
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "BrandNewSecretPassword123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("FR-AUTH-11 api major version is published for client compatibility")
    void actuatorInfo_publishesApiMajorVersion() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.api-version").value(1));
    }

    // ---- helpers ---------------------------------------------------------------------------

    ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", refreshToken))));
    }

    ResultActions logout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", refreshToken))));
    }

    java.sql.Timestamp revokedAt(String refreshToken) {
        return jdbc.queryForObject("select revoked_at from refresh_tokens where token_hash = ?",
                java.sql.Timestamp.class, com.nexora.identity.application.RefreshTokenHasher.hash(refreshToken));
    }

    ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))));
    }

    String detail(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("detail").asString();
    }

    int failedAttempts(String username) {
        return jdbc.queryForObject("select failed_login_attempts from users where username = ?", Integer.class, username);
    }
}
