package com.apiscope.sample.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

/**
 * Simulated inventory repository.
 * In a real app this would call a database — here it returns deterministic mock data
 * so the Flow Tracer can show a REPOSITORY layer step in the execution diagram.
 */
@Repository
public class InventoryRepository {

    /**
     * Look up current stock level for a product.
     *
     * @param productId the product identifier
     * @return stock record with available quantity and warehouse location
     */
    public Map<String, Object> findStockByProductId(String productId) {
        // Simulate a DB query latency
        simulateLatency(15);
        return Map.of(
                "productId",    productId,
                "available",    42,
                "reserved",     3,
                "warehouse",    "WH-EAST-01",
                "reorderLevel", 10
        );
    }

    /**
     * Reserve stock for an order line item — decrements available, increments reserved.
     *
     * @param productId the product to reserve
     * @param quantity  how many units to reserve
     * @return reservation confirmation with a reservation token
     */
    public Map<String, Object> reserveStock(String productId, int quantity) {
        simulateLatency(20);
        return Map.of(
                "productId",         productId,
                "quantityReserved",  quantity,
                "reservationToken",  "RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "expiresInSeconds",  300
        );
    }

    private void simulateLatency(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
