package com.quickbite.identity;

import com.quickbite.identity.dto.AuthResponse;
import com.quickbite.identity.dto.LoginRequest;
import com.quickbite.identity.dto.SignupRequest;
import com.quickbite.security.CurrentAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Identity", description = "Endpoints for user registration, login, logout, and profile lookup")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Register profile", description = "Provisions a Buyer, Delivery Partner, Shop Owner, or Admin (with env key or pending admin approval)")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.registerProfile(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password", description = "Authenticates a registered Buyer, Shop Owner, Delivery Partner, or Admin account")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out", description = "Audits account logout for the active bearer session")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated profile", description = "Retrieves the active profile, role, and verification state for the current Bearer token")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CurrentAccountPrincipal principal) {
        AuthResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(response);
    }
}
