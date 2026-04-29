package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication & Authorization API — login, token refresh, OAuth, MFA, and API keys.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Authorization")
public class AuthController {

    @Operation(summary = "Login with email and password. Request body: { email, password, deviceId }. Returns accessToken and refreshToken.")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "accessToken", "eyJhbGciOiJSUzI1NiJ9...",
                "refreshToken", UUID.randomUUID().toString(),
                "expiresIn", 3600,
                "tokenType", "Bearer"
        ));
    }

    @Operation(summary = "Refresh an expired access token using a valid refresh token. Request body: { refreshToken }")
    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "accessToken", "eyJhbGciOiJSUzI1NiJ9...",
                "expiresIn", 3600,
                "tokenType", "Bearer"
        ));
    }

    @Operation(summary = "Logout and invalidate the current access token. Request body: { refreshToken }")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("loggedOut", true));
    }

    @Operation(summary = "Initiate password reset — sends a reset link to the registered email. Request body: { email }")
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("message", "Reset link sent", "email", request.getOrDefault("email", "")));
    }

    @Operation(summary = "Reset password using the token from the email link. Request body: { token, newPassword }")
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("passwordReset", true));
    }

    @Operation(summary = "Enable MFA for an account. Returns a TOTP QR code URL and secret. Request body: { userId, method: TOTP|SMS }")
    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, Object>> enableMfa(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "mfaEnabled", false,
                "pendingVerification", true,
                "qrCodeUrl", "otpauth://totp/AgenticDocs?secret=BASE32SECRET",
                "secret", "BASE32SECRET"
        ));
    }

    @Operation(summary = "Verify MFA code to complete MFA setup or login. Request body: { userId, code }")
    @PostMapping("/mfa/verify")
    public ResponseEntity<Map<String, Object>> verifyMfa(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("verified", true, "mfaFullyEnabled", true));
    }

    @Operation(summary = "Create an API key for programmatic access. Request body: { userId, name, scopes, expiresAt }")
    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "keyId", UUID.randomUUID().toString(),
                "apiKey", "ak_live_" + UUID.randomUUID().toString().replace("-", ""),
                "name", request.getOrDefault("name", "My Key"),
                "createdAt", "2026-04-29T00:00:00Z"
        ));
    }

    @Operation(summary = "List all API keys for a user. Query params: userId, active")
    @GetMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> listApiKeys(
            @RequestParam String userId,
            @RequestParam(defaultValue = "true") boolean active) {
        return ResponseEntity.ok(Map.of("userId", userId, "apiKeys", List.of()));
    }

    @Operation(summary = "Revoke (delete) an API key permanently")
    @DeleteMapping("/api-keys/{keyId}")
    public ResponseEntity<Map<String, Object>> revokeApiKey(@PathVariable String keyId) {
        return ResponseEntity.ok(Map.of("keyId", keyId, "revoked", true));
    }
}
