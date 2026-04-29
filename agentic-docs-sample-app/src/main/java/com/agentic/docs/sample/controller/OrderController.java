package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Order Management API — create, track, cancel, and return orders.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management")
public class OrderController {

    @Operation(summary = "Create a new order. Request body: { customerId, items: [{productId, quantity, unitPrice}], shippingAddress, paymentMethodId }")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "orderId", UUID.randomUUID().toString(),
                "status", "PENDING",
                "estimatedDelivery", "2026-05-05"
        ));
    }

    @Operation(summary = "Get order details by orderId including items, status, and tracking info")
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "SHIPPED",
                "items", List.of(Map.of("productId", "P001", "quantity", 2, "unitPrice", 29.99)),
                "total", 59.98,
                "trackingNumber", "TRK123456789"
        ));
    }

    @Operation(summary = "List all orders for a customer. Query params: status, page, size, fromDate, toDate")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(
            @PathVariable String customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("customerId", customerId, "orders", List.of(), "total", 0));
    }

    @Operation(summary = "Cancel an order. Only PENDING or PROCESSING orders can be cancelled. Request body: { reason }")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("orderId", orderId, "status", "CANCELLED", "refundInitiated", true));
    }

    @Operation(summary = "Update shipping address for a PENDING order. Request body: { street, city, state, zip, country }")
    @PutMapping("/{orderId}/shipping-address")
    public ResponseEntity<Map<String, Object>> updateShippingAddress(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("orderId", orderId, "addressUpdated", true));
    }

    @Operation(summary = "Initiate a return for a delivered order. Request body: { items: [{orderItemId, quantity, reason}] }")
    @PostMapping("/{orderId}/return")
    public ResponseEntity<Map<String, Object>> initiateReturn(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "returnId", UUID.randomUUID().toString(),
                "orderId", orderId,
                "status", "RETURN_INITIATED",
                "returnLabel", "https://returns.example.com/label/RL001"
        ));
    }

    @Operation(summary = "Get live tracking status for a shipped order")
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<Map<String, Object>> getTracking(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "carrier", "FedEx",
                "trackingNumber", "TRK123456789",
                "currentLocation", "Chicago, IL",
                "estimatedDelivery", "2026-05-05",
                "events", List.of(
                        Map.of("timestamp", "2026-04-29T10:00:00Z", "status", "In Transit", "location", "Chicago, IL")
                )
        ));
    }

    @Operation(summary = "Apply a discount coupon to a pending order. Request body: { couponCode }")
    @PostMapping("/{orderId}/apply-coupon")
    public ResponseEntity<Map<String, Object>> applyCoupon(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("orderId", orderId, "discount", 10.00, "newTotal", 49.98));
    }

    @Operation(summary = "Get order invoice as a downloadable link")
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<Map<String, Object>> getInvoice(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "invoiceUrl", "https://invoices.example.com/INV-" + orderId + ".pdf",
                "generatedAt", "2026-04-29T12:00:00Z"
        ));
    }

    @Operation(summary = "Reorder — clone a previous order into a new PENDING order")
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<Map<String, Object>> reorder(@PathVariable String orderId) {
        return ResponseEntity.status(201).body(Map.of(
                "newOrderId", UUID.randomUUID().toString(),
                "clonedFromOrderId", orderId,
                "status", "PENDING"
        ));
    }
}
