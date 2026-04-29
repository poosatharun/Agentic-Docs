package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shipping & Logistics API — carriers, shipments, rates, labels, and delivery tracking.
 */
@RestController
@RequestMapping("/api/v1/shipping")
@Tag(name = "Shipping & Logistics")
public class ShippingController {

    @Operation(summary = "Get shipping rates for a package. Request body: { fromZip, toZip, weightKg, dimensionsCm: {l,w,h}, carriers }")
    @PostMapping("/rates")
    public ResponseEntity<Map<String, Object>> getShippingRates(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("rates", List.of(
                Map.of("carrier", "FedEx", "service", "Ground", "price", 8.99, "estimatedDays", 5),
                Map.of("carrier", "UPS", "service", "2-Day Air", "price", 22.50, "estimatedDays", 2),
                Map.of("carrier", "USPS", "service", "Priority Mail", "price", 7.45, "estimatedDays", 3)
        )));
    }

    @Operation(summary = "Create a shipment and generate a shipping label. Request body: { orderId, carrierId, serviceCode, fromAddress, toAddress, packageDetails }")
    @PostMapping("/shipments")
    public ResponseEntity<Map<String, Object>> createShipment(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "shipmentId", UUID.randomUUID().toString(),
                "trackingNumber", "TRK" + System.currentTimeMillis(),
                "labelUrl", "https://labels.example.com/label.pdf",
                "carrier", "FedEx",
                "estimatedDelivery", "2026-05-04"
        ));
    }

    @Operation(summary = "Get shipment details and current status by shipmentId")
    @GetMapping("/shipments/{shipmentId}")
    public ResponseEntity<Map<String, Object>> getShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(Map.of(
                "shipmentId", shipmentId,
                "status", "IN_TRANSIT",
                "carrier", "FedEx",
                "trackingNumber", "TRK123",
                "currentLocation", "Memphis, TN"
        ));
    }

    @Operation(summary = "Void (cancel) a shipment label before it is scanned by the carrier")
    @DeleteMapping("/shipments/{shipmentId}")
    public ResponseEntity<Map<String, Object>> voidShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(Map.of("shipmentId", shipmentId, "voided", true, "refunded", true));
    }

    @Operation(summary = "Track a shipment by tracking number. Query params: carrier, trackingNumber")
    @GetMapping("/track")
    public ResponseEntity<Map<String, Object>> trackByNumber(
            @RequestParam String carrier,
            @RequestParam String trackingNumber) {
        return ResponseEntity.ok(Map.of(
                "carrier", carrier,
                "trackingNumber", trackingNumber,
                "status", "OUT_FOR_DELIVERY",
                "events", List.of(
                        Map.of("timestamp", "2026-04-29T08:00:00Z", "status", "Out for Delivery", "location", "Austin, TX")
                )
        ));
    }

    @Operation(summary = "Create a return label for a delivered shipment. Request body: { orderId, shipmentId, reason, fromAddress }")
    @PostMapping("/returns/label")
    public ResponseEntity<Map<String, Object>> createReturnLabel(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "returnLabelId", UUID.randomUUID().toString(),
                "labelUrl", "https://returns.example.com/return-label.pdf",
                "expiresAt", "2026-05-29"
        ));
    }

    @Operation(summary = "Get all carrier accounts configured in the system")
    @GetMapping("/carriers")
    public ResponseEntity<Map<String, Object>> getCarriers() {
        return ResponseEntity.ok(Map.of("carriers", List.of(
                Map.of("carrierId", "FEDEX", "name", "FedEx", "active", true),
                Map.of("carrierId", "UPS", "name", "UPS", "active", true),
                Map.of("carrierId", "USPS", "name", "USPS", "active", true)
        )));
    }

    @Operation(summary = "Get delivery performance report by carrier. Query params: fromDate, toDate, carrier")
    @GetMapping("/reports/performance")
    public ResponseEntity<Map<String, Object>> getCarrierPerformance(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String carrier) {
        return ResponseEntity.ok(Map.of(
                "onTimeDeliveryRate", "96.8%",
                "averageDeliveryDays", 3.2,
                "lostPackages", 5,
                "damagedPackages", 12
        ));
    }

    @Operation(summary = "Get shipping zones and transit times between origin and destination zips. Query params: fromZip, toZip")
    @GetMapping("/zones")
    public ResponseEntity<Map<String, Object>> getShippingZone(
            @RequestParam String fromZip,
            @RequestParam String toZip) {
        return ResponseEntity.ok(Map.of(
                "fromZip", fromZip,
                "toZip", toZip,
                "zone", 4,
                "estimatedTransitDays", Map.of("FedEx", 4, "UPS", 3, "USPS", 5)
        ));
    }

    @Operation(summary = "Bulk create shipments from a list of orders. Request body: { orderIds, carrierId, serviceCode }")
    @PostMapping("/shipments/bulk")
    public ResponseEntity<Map<String, Object>> bulkCreateShipments(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "batchId", UUID.randomUUID().toString(),
                "status", "PROCESSING",
                "estimatedCompletionSeconds", 30
        ));
    }
}
