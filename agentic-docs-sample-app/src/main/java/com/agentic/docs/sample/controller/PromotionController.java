package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Promotions & Coupons API — discount campaigns, coupon codes, flash sales, and loyalty rewards.
 */
@RestController
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promotions & Coupons")
public class PromotionController {

    @Operation(summary = "Create a new promotion campaign. Request body: { name, type: PERCENTAGE|FIXED|FREE_SHIPPING|BUY_X_GET_Y, value, conditions, startDate, endDate, maxUses }")
    @PostMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "campaignId", UUID.randomUUID().toString(),
                "name", request.getOrDefault("name", ""),
                "status", "SCHEDULED"
        ));
    }

    @Operation(summary = "Get campaign details by campaignId including usage stats")
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<Map<String, Object>> getCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(Map.of(
                "campaignId", campaignId,
                "name", "Spring Sale 2026",
                "type", "PERCENTAGE",
                "value", 20,
                "status", "ACTIVE",
                "usesCount", 1240,
                "maxUses", 5000
        ));
    }

    @Operation(summary = "List all campaigns. Query params: status (ACTIVE|SCHEDULED|EXPIRED), page, size")
    @GetMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("campaigns", List.of(), "total", 0));
    }

    @Operation(summary = "Deactivate a campaign immediately")
    @PostMapping("/campaigns/{campaignId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(Map.of("campaignId", campaignId, "status", "DEACTIVATED"));
    }

    @Operation(summary = "Generate bulk coupon codes for a campaign. Request body: { campaignId, count, prefix, expiresAt }")
    @PostMapping("/coupons/generate")
    public ResponseEntity<Map<String, Object>> generateCoupons(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "batchId", UUID.randomUUID().toString(),
                "campaignId", request.getOrDefault("campaignId", ""),
                "generated", request.getOrDefault("count", 0),
                "status", "PROCESSING"
        ));
    }

    @Operation(summary = "Validate a coupon code for a cart. Request body: { couponCode, cartTotal, customerId, items }")
    @PostMapping("/coupons/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "couponCode", request.getOrDefault("couponCode", ""),
                "discountAmount", 15.00,
                "newTotal", 84.99
        ));
    }

    @Operation(summary = "Get coupon usage history. Query params: couponCode, fromDate, toDate, page, size")
    @GetMapping("/coupons/usage")
    public ResponseEntity<Map<String, Object>> getCouponUsage(
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(Map.of("usages", List.of(), "total", 0));
    }

    @Operation(summary = "Get loyalty points balance and tier for a customer. Query params: customerId")
    @GetMapping("/loyalty/points")
    public ResponseEntity<Map<String, Object>> getLoyaltyPoints(@RequestParam String customerId) {
        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "points", 2450,
                "tier", "GOLD",
                "pointsToNextTier", 550,
                "expiringPoints", 100,
                "expiringDate", "2026-06-30"
        ));
    }

    @Operation(summary = "Redeem loyalty points for a discount. Request body: { customerId, points, orderId }")
    @PostMapping("/loyalty/redeem")
    public ResponseEntity<Map<String, Object>> redeemLoyaltyPoints(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "redeemed", true,
                "pointsUsed", request.getOrDefault("points", 0),
                "discountAmount", 12.25,
                "remainingPoints", 2000
        ));
    }

    @Operation(summary = "Create a flash sale for specific products or categories. Request body: { name, productIds, discountPercentage, startTime, endTime, maxUnitsPerCustomer }")
    @PostMapping("/flash-sales")
    public ResponseEntity<Map<String, Object>> createFlashSale(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "flashSaleId", UUID.randomUUID().toString(),
                "name", request.getOrDefault("name", ""),
                "status", "SCHEDULED",
                "startTime", request.getOrDefault("startTime", ""),
                "endTime", request.getOrDefault("endTime", "")
        ));
    }
}
