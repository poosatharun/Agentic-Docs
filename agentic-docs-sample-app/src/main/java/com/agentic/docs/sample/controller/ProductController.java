package com.agentic.docs.sample.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Product Catalog API — product CRUD, search, inventory, pricing, and reviews.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "productId", UUID.randomUUID().toString(),
                "sku", request.getOrDefault("sku", "SKU-001"),
                "status", "DRAFT"
        ));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "name", "Wireless Headphones Pro",
                "sku", "WH-PRO-001",
                "category", "Electronics",
                "price", 149.99,
                "stockQuantity", 250,
                "status", "ACTIVE"
        ));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("productId", productId, "updated", true));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deactivateProduct(@PathVariable String productId) {
        return ResponseEntity.ok(Map.of("productId", productId, "status", "DEACTIVATED"));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "true") boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy) {
        return ResponseEntity.ok(Map.of("results", List.of(), "total", 0, "page", page, "size", size));
    }

    @PatchMapping("/{productId}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("productId", productId, "newStockQuantity", 300));
    }

    @GetMapping("/{productId}/stock/history")
    public ResponseEntity<Map<String, Object>> getStockHistory(
            @PathVariable String productId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(Map.of("productId", productId, "movements", List.of()));
    }

    @PostMapping("/{productId}/reviews")
    public ResponseEntity<Map<String, Object>> addReview(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "reviewId", UUID.randomUUID().toString(),
                "productId", productId,
                "rating", request.getOrDefault("rating", 5),
                "status", "PENDING_MODERATION"
        ));
    }

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<Map<String, Object>> getReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recent") String sortBy) {
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "reviews", List.of(),
                "averageRating", 4.5,
                "totalReviews", 0
        ));
    }

    @PutMapping("/{productId}/pricing")
    public ResponseEntity<Map<String, Object>> updatePricing(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "price", request.getOrDefault("price", 149.99),
                "salePrice", request.getOrDefault("salePrice", null),
                "priceUpdated", true
        ));
    }
}
