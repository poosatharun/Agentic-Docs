package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reporting & Analytics API — dashboards, KPIs, exports, and scheduled reports.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Reporting & Analytics")
public class AnalyticsController {

    @Operation(summary = "Get revenue summary for a date range. Query params: fromDate, toDate, groupBy: DAY|WEEK|MONTH, currency")
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getRevenue(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(defaultValue = "DAY") String groupBy,
            @RequestParam(defaultValue = "USD") String currency) {
        return ResponseEntity.ok(Map.of(
                "totalRevenue", 1_234_567.89,
                "currency", currency,
                "groupBy", groupBy,
                "series", List.of()
        ));
    }

    @Operation(summary = "Get top-selling products report. Query params: fromDate, toDate, limit, category")
    @GetMapping("/products/top-selling")
    public ResponseEntity<Map<String, Object>> getTopSellingProducts(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(Map.of("products", List.of(), "period", Map.of("from", fromDate, "to", toDate)));
    }

    @Operation(summary = "Get customer acquisition and churn metrics. Query params: fromDate, toDate, segment")
    @GetMapping("/customers/metrics")
    public ResponseEntity<Map<String, Object>> getCustomerMetrics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String segment) {
        return ResponseEntity.ok(Map.of(
                "newCustomers", 1450,
                "churned", 230,
                "netGrowth", 1220,
                "churnRate", "2.3%"
        ));
    }

    @Operation(summary = "Get order fulfilment and SLA metrics. Query params: fromDate, toDate, warehouseId")
    @GetMapping("/fulfilment/metrics")
    public ResponseEntity<Map<String, Object>> getFulfilmentMetrics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String warehouseId) {
        return ResponseEntity.ok(Map.of(
                "ordersProcessed", 23400,
                "onTimeDeliveryRate", "96.2%",
                "averageFulfilmentHours", 18.4,
                "returnRate", "3.1%"
        ));
    }

    @Operation(summary = "Get real-time dashboard KPIs snapshot — orders, revenue, active users, stock alerts")
    @GetMapping("/dashboard/kpis")
    public ResponseEntity<Map<String, Object>> getDashboardKpis() {
        return ResponseEntity.ok(Map.of(
                "ordersToday", 847,
                "revenueToday", 42_350.00,
                "activeUsers", 3210,
                "lowStockAlerts", 14,
                "pendingRefunds", 23,
                "asOf", "2026-04-29T15:00:00Z"
        ));
    }

    @Operation(summary = "Export a report as CSV or Excel. Request body: { reportType: REVENUE|ORDERS|CUSTOMERS|INVENTORY, fromDate, toDate, format: CSV|XLSX }")
    @PostMapping("/reports/export")
    public ResponseEntity<Map<String, Object>> exportReport(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "exportId", UUID.randomUUID().toString(),
                "status", "PROCESSING",
                "format", request.getOrDefault("format", "CSV"),
                "downloadUrlAvailableIn", "30 seconds"
        ));
    }

    @Operation(summary = "Download a previously generated report export by exportId")
    @GetMapping("/reports/export/{exportId}/download")
    public ResponseEntity<Map<String, Object>> downloadExport(@PathVariable String exportId) {
        return ResponseEntity.ok(Map.of(
                "exportId", exportId,
                "downloadUrl", "https://exports.example.com/" + exportId + ".csv",
                "expiresAt", "2026-04-29T16:00:00Z"
        ));
    }

    @Operation(summary = "Schedule a recurring report. Request body: { reportType, schedule: DAILY|WEEKLY|MONTHLY, recipients, format, filters }")
    @PostMapping("/reports/schedule")
    public ResponseEntity<Map<String, Object>> scheduleReport(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "scheduleId", UUID.randomUUID().toString(),
                "reportType", request.getOrDefault("reportType", ""),
                "schedule", request.getOrDefault("schedule", "WEEKLY"),
                "nextRun", "2026-05-06T06:00:00Z"
        ));
    }

    @Operation(summary = "Get cohort retention analysis. Query params: cohortMonth (YYYY-MM), periods (number of months)")
    @GetMapping("/customers/cohort-retention")
    public ResponseEntity<Map<String, Object>> getCohortRetention(
            @RequestParam String cohortMonth,
            @RequestParam(defaultValue = "6") int periods) {
        return ResponseEntity.ok(Map.of(
                "cohortMonth", cohortMonth,
                "retentionByPeriod", List.of()
        ));
    }

    @Operation(summary = "Get payment failure analysis — decline rates by reason, card type, and geography. Query params: fromDate, toDate")
    @GetMapping("/payments/failure-analysis")
    public ResponseEntity<Map<String, Object>> getPaymentFailureAnalysis(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(Map.of(
                "totalDeclined", 320,
                "declineRate", "1.4%",
                "topReasons", List.of(
                        Map.of("reason", "INSUFFICIENT_FUNDS", "count", 145),
                        Map.of("reason", "CARD_EXPIRED", "count", 88)
                )
        ));
    }
}
