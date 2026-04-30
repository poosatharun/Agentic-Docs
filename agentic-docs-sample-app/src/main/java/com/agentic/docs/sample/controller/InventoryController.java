package com.agentic.docs.sample.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inventory & Warehouse API — warehouse management, stock levels, transfers, and alerts.
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    @GetMapping("/stock")
    public ResponseEntity<Map<String, Object>> getStockLevels(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String warehouseId) {
        return ResponseEntity.ok(Map.of(
                "sku", sku,
                "totalStock", 1500,
                "warehouses", List.of(
                        Map.of("warehouseId", "WH-001", "location", "Chicago", "quantity", 800),
                        Map.of("warehouseId", "WH-002", "location", "Dallas", "quantity", 700)
                )
        ));
    }

    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> createTransfer(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "transferId", UUID.randomUUID().toString(),
                "status", "PENDING",
                "estimatedArrival", "2026-05-02"
        ));
    }

    @GetMapping("/transfers/{transferId}")
    public ResponseEntity<Map<String, Object>> getTransfer(@PathVariable String transferId) {
        return ResponseEntity.ok(Map.of(
                "transferId", transferId,
                "status", "IN_TRANSIT",
                "sku", "WH-PRO-001",
                "quantity", 100
        ));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<Map<String, Object>> listWarehouses() {
        return ResponseEntity.ok(Map.of("warehouses", List.of(
                Map.of("warehouseId", "WH-001", "location", "Chicago, IL", "capacity", 10000, "utilisation", 0.80),
                Map.of("warehouseId", "WH-002", "location", "Dallas, TX", "capacity", 8000, "utilisation", 0.65)
        )));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Map<String, Object>> adjustStock(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "adjustmentId", UUID.randomUUID().toString(),
                "sku", request.getOrDefault("sku", ""),
                "delta", request.getOrDefault("delta", 0),
                "reason", request.getOrDefault("reason", "AUDIT")
        ));
    }

    @GetMapping("/alerts/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockAlerts(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(defaultValue = "50") int threshold) {
        return ResponseEntity.ok(Map.of("alerts", List.of(), "threshold", threshold));
    }

    @PutMapping("/reorder-rules")
    public ResponseEntity<Map<String, Object>> setReorderRule(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("ruleId", UUID.randomUUID().toString(), "saved", true));
    }

    @GetMapping("/reports/valuation")
    public ResponseEntity<Map<String, Object>> getValuationReport(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(defaultValue = "FIFO") String valuationMethod) {
        return ResponseEntity.ok(Map.of(
                "totalValue", 2_450_000.00,
                "currency", "USD",
                "valuationMethod", valuationMethod,
                "asOf", "2026-04-29"
        ));
    }

    @PostMapping("/receipts")
    public ResponseEntity<Map<String, Object>> receiveShipment(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "receiptId", UUID.randomUUID().toString(),
                "purchaseOrderId", request.getOrDefault("purchaseOrderId", ""),
                "status", "RECEIVED"
        ));
    }

    @GetMapping("/movements")
    public ResponseEntity<Map<String, Object>> getMovements(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(Map.of("movements", List.of(), "total", 0));
    }
}
