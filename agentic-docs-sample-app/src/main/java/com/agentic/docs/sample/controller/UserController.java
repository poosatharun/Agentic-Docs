package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User Management API — registration, profile, roles, deactivation, and search.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management")
public class UserController {

    @Operation(summary = "Register a new user. Request body: { email, password, firstName, lastName, role }")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "userId", UUID.randomUUID().toString(),
                "email", request.getOrDefault("email", "user@example.com"),
                "status", "PENDING_VERIFICATION"
        ));
    }

    @Operation(summary = "Get a user profile by userId")
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "email", "john.doe@example.com",
                "firstName", "John",
                "lastName", "Doe",
                "role", "CUSTOMER",
                "status", "ACTIVE"
        ));
    }

    @Operation(summary = "Update a user profile. Request body: { firstName, lastName, phone, address }")
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("userId", userId, "updated", true));
    }

    @Operation(summary = "Deactivate a user account. Sets status to DEACTIVATED and revokes all tokens.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("userId", userId, "status", "DEACTIVATED"));
    }

    @Operation(summary = "Search users by email, name, or role. Query params: query, role, page, size")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("results", List.of(), "total", 0, "page", page, "size", size));
    }

    @Operation(summary = "Verify a user's email address using the token sent on registration. Request body: { token }")
    @PostMapping("/{userId}/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("userId", userId, "emailVerified", true));
    }

    @Operation(summary = "Assign a role to a user. Request body: { role: ADMIN|MANAGER|CUSTOMER|SUPPORT }")
    @PutMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("userId", userId, "role", request.getOrDefault("role", "CUSTOMER")));
    }

    @Operation(summary = "Change a user's password. Request body: { currentPassword, newPassword }")
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("userId", userId, "passwordChanged", true));
    }

    @Operation(summary = "Get all login sessions for a user. Query params: active (boolean)")
    @GetMapping("/{userId}/sessions")
    public ResponseEntity<Map<String, Object>> getUserSessions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "true") boolean active) {
        return ResponseEntity.ok(Map.of("userId", userId, "sessions", List.of(), "activeOnly", active));
    }

    @Operation(summary = "Revoke all active sessions for a user (force logout from all devices).")
    @DeleteMapping("/{userId}/sessions")
    public ResponseEntity<Map<String, Object>> revokeAllSessions(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("userId", userId, "sessionsRevoked", true));
    }
}
