package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vendor & Supplier Management API — onboarding, purchase orders, contracts, and performance.
 */
@RestController
@RequestMapping("/api/v1/vendors")
@Tag(name = "Vendor & Supplier Management")
public class VendorController {

    @Operation(summary = "Onboard a new vendor. Request body: { companyName, contactEmail, contactPhone, address, taxId, paymentTerms, categories }")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVendor(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "vendorId", UUID.randomUUID().toString(),
                "companyName", request.getOrDefault("companyName", ""),
                "status", "PENDING_APPROVAL"
        ));
    }

    @Operation(summary = "Get vendor details by vendorId including contracts, payment terms, and performance score")
    @GetMapping("/{vendorId}")
    public ResponseEntity<Map<String, Object>> getVendor(@PathVariable String vendorId) {
        return ResponseEntity.ok(Map.of(
                "vendorId", vendorId,
                "companyName", "Acme Supplies Ltd",
                "status", "APPROVED",
                "performanceScore", 87,
                "onTimeDeliveryRate", "94.5%",
                "activeContracts", 3
        ));
    }

    @Operation(summary = "Update vendor profile. Request body: { contactEmail, contactPhone, address, paymentTerms }")
    @PutMapping("/{vendorId}")
    public ResponseEntity<Map<String, Object>> updateVendor(
            @PathVariable String vendorId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("vendorId", vendorId, "updated", true));
    }

    @Operation(summary = "Approve or reject a vendor application. Request body: { decision: APPROVED|REJECTED, notes }")
    @PostMapping("/{vendorId}/review")
    public ResponseEntity<Map<String, Object>> reviewVendor(
            @PathVariable String vendorId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("vendorId", vendorId, "status", request.getOrDefault("decision", "PENDING")));
    }

    @Operation(summary = "Create a purchase order for a vendor. Request body: { vendorId, items: [{sku, quantity, unitCost}], deliveryDate, warehouseId, notes }")
    @PostMapping("/purchase-orders")
    public ResponseEntity<Map<String, Object>> createPurchaseOrder(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "poId", "PO-" + System.currentTimeMillis(),
                "vendorId", request.getOrDefault("vendorId", ""),
                "status", "SENT",
                "totalCost", 15000.00
        ));
    }

    @Operation(summary = "Get purchase order details by poId")
    @GetMapping("/purchase-orders/{poId}")
    public ResponseEntity<Map<String, Object>> getPurchaseOrder(@PathVariable String poId) {
        return ResponseEntity.ok(Map.of(
                "poId", poId,
                "status", "CONFIRMED",
                "items", List.of(),
                "totalCost", 15000.00,
                "deliveryDate", "2026-05-15"
        ));
    }

    @Operation(summary = "List purchase orders. Query params: vendorId, status, fromDate, toDate, page, size")
    @GetMapping("/purchase-orders")
    public ResponseEntity<Map<String, Object>> listPurchaseOrders(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of("purchaseOrders", List.of(), "total", 0));
    }

    @Operation(summary = "Get vendor performance scorecard. Query params: vendorId, fromDate, toDate")
    @GetMapping("/{vendorId}/scorecard")
    public ResponseEntity<Map<String, Object>> getScorecard(
            @PathVariable String vendorId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(Map.of(
                "vendorId", vendorId,
                "overallScore", 87,
                "onTimeDelivery", "94.5%",
                "qualityRating", 4.2,
                "invoiceAccuracy", "99.1%",
                "responseTime", "12 hours"
        ));
    }

    @Operation(summary = "Upload and attach a vendor contract document. Request body: { vendorId, contractType, startDate, endDate, fileUrl }")
    @PostMapping("/{vendorId}/contracts")
    public ResponseEntity<Map<String, Object>> uploadContract(
            @PathVariable String vendorId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "contractId", UUID.randomUUID().toString(),
                "vendorId", vendorId,
                "status", "ACTIVE",
                "expiresAt", request.getOrDefault("endDate", "")
        ));
    }

    @Operation(summary = "Process a vendor invoice and schedule payment. Request body: { vendorId, poId, invoiceNumber, amount, currency, dueDate, lineItems }")
    @PostMapping("/invoices")
    public ResponseEntity<Map<String, Object>> processInvoice(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "status", "APPROVED",
                "paymentScheduled", request.getOrDefault("dueDate", ""),
                "amount", request.getOrDefault("amount", 0)
        ));
    }
}
