package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notification Service API — send, schedule, and manage email/SMS/push notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification Service")
public class NotificationController {

    @Operation(summary = "Send an immediate email notification. Request body: { to, subject, templateId, templateVars, priority: HIGH|NORMAL|LOW }")
    @PostMapping("/email/send")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "notificationId", UUID.randomUUID().toString(),
                "channel", "EMAIL",
                "status", "QUEUED",
                "to", request.getOrDefault("to", "")
        ));
    }

    @Operation(summary = "Send an SMS notification. Request body: { phoneNumber, message, senderId }")
    @PostMapping("/sms/send")
    public ResponseEntity<Map<String, Object>> sendSms(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "notificationId", UUID.randomUUID().toString(),
                "channel", "SMS",
                "status", "QUEUED"
        ));
    }

    @Operation(summary = "Send a push notification to a device or user. Request body: { userId, title, body, data, deviceTokens }")
    @PostMapping("/push/send")
    public ResponseEntity<Map<String, Object>> sendPush(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "notificationId", UUID.randomUUID().toString(),
                "channel", "PUSH",
                "status", "QUEUED",
                "devicesTargeted", 1
        ));
    }

    @Operation(summary = "Schedule a notification for a future time. Request body: { channel, recipient, scheduledAt (ISO-8601), templateId, templateVars }")
    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> scheduleNotification(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "scheduleId", UUID.randomUUID().toString(),
                "scheduledAt", request.getOrDefault("scheduledAt", ""),
                "status", "SCHEDULED"
        ));
    }

    @Operation(summary = "Cancel a scheduled notification before it is sent")
    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Map<String, Object>> cancelScheduled(@PathVariable String scheduleId) {
        return ResponseEntity.ok(Map.of("scheduleId", scheduleId, "cancelled", true));
    }

    @Operation(summary = "Get notification delivery status by notificationId")
    @GetMapping("/{notificationId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String notificationId) {
        return ResponseEntity.ok(Map.of(
                "notificationId", notificationId,
                "status", "DELIVERED",
                "deliveredAt", "2026-04-29T12:05:00Z",
                "channel", "EMAIL"
        ));
    }

    @Operation(summary = "Get notification history for a user. Query params: userId, channel, page, size, fromDate, toDate")
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("userId", userId, "notifications", List.of(), "total", 0));
    }

    @Operation(summary = "Create or update a notification template. Request body: { templateId, name, channel, subject, bodyHtml, bodyText, variables }")
    @PutMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> upsertTemplate(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("templateId", templateId, "saved", true));
    }

    @Operation(summary = "Update user notification preferences. Request body: { userId, emailEnabled, smsEnabled, pushEnabled, marketingOptIn }")
    @PutMapping("/preferences/{userId}")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("userId", userId, "preferencesUpdated", true));
    }

    @Operation(summary = "Get notification delivery metrics — sent, delivered, bounced, opened. Query params: fromDate, toDate, channel, templateId")
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String templateId) {
        return ResponseEntity.ok(Map.of(
                "sent", 15000,
                "delivered", 14800,
                "bounced", 50,
                "opened", 8200,
                "openRate", "55.4%"
        ));
    }
}
