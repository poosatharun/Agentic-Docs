package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Demo Payments & Subscriptions API.
 * All endpoints are indexed by Agentic Docs and available for RAG queries.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments & Subscriptions")
public class PaymentsController {

    @Operation(summary = "Get full subscription details by ID, including daysActive, plan, and status")
    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<Map<String, Object>> getSubscription(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "ACTIVE",
                "plan", "PREMIUM",
                "daysActive", 10
        ));
    }

    @Operation(summary = "List all subscriptions for an account")
    @GetMapping("/accounts/{accountId}/subscriptions")
    public ResponseEntity<Map<String, Object>> listSubscriptions(@PathVariable String accountId) {
        return ResponseEntity.ok(Map.of("accountId", accountId, "subscriptions", List.of()));
    }

    @Operation(summary = "Terminate a subscription. Request body: { refundType: PARTIAL|FULL|NONE, isProrated: boolean }")
    @PostMapping("/subscriptions/{id}/terminate")
    public ResponseEntity<Map<String, Object>> terminateSubscription(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("subscriptionId", id, "status", "TERMINATED", "refundType", request.get("refundType")));
    }

    @Operation(summary = "Upgrade or downgrade a subscription plan. Request body: { newPlan: string }")
    @PutMapping("/subscriptions/{id}/plan")
    public ResponseEntity<Map<String, Object>> changePlan(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("id", id, "newPlan", request.get("newPlan")));
    }

    @Operation(summary = "Calculate refund amount for a subscription based on daysActive and plan. Request body: { subscriptionId, daysActive, plan }")
    @PostMapping("/billing/refund/calculate")
    public ResponseEntity<Map<String, Object>> calculateRefund(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("refundAmount", 14.99, "currency", "USD"));
    }

    @Operation(summary = "Process a payment. Request body: { orderId, amount, currency, paymentMethod: CARD|WALLET }")
    @PostMapping("/payments/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("transactionId", "TXN-001", "status", "SUCCESS"));
    }

    @Operation(summary = "Get payment history for an account, paginated. Query params: page, size")
    @GetMapping("/accounts/{accountId}/payments")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("accountId", accountId, "page", page, "size", size, "transactions", List.of()));
    }

    @Operation(summary = "Issue a manual refund for a transaction. Request body: { transactionId, amount, reason }")
    @PostMapping("/payments/refund")
    public ResponseEntity<Map<String, Object>> issueRefund(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("refundId", "REF-001", "status", "PROCESSED"));
    }
}
