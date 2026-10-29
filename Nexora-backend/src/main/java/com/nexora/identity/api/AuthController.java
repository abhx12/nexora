package com.nexora.identity.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.identity.api.dto.LoginRequest;
import com.nexora.identity.api.dto.MeResponse;
import com.nexora.shared.security.CurrentUser;
import com.nexora.identity.api.dto.RefreshRequest;
import com.nexora.identity.api.dto.TokenResponse;
import com.nexora.identity.application.AuthService;

import jakarta.validation.Valid;

// Authentication endpoints. login, refresh and logout are public (permitAll in SecurityConfig):
// the caller has no access token yet, that is what these endpoints are for.
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    // Any logged-in user may read their own profile; no specific permission is needed.
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    MeResponse me() {
        return authService.me(CurrentUser.id());
    }

    // 204 No Content: logout has nothing to return.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    void changePassword(@Valid @RequestBody com.nexora.identity.api.dto.ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.id(), request);
    }
}
