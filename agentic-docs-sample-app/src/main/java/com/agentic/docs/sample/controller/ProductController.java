package com.agentic.docs.sample.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Product Catalog")
public class ProductController {

    @Operation(summary = "Create a new product. Request body: { name, sku, category, price, stockQuantity, description, imageUrls }")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(201).body(Map.of(
                "productId", UUID.randomUUID().toString(),
                "sku", request.getOrDefault("sku", "SKU-001"),
                "status", "DRAFT"
        ));
    }

    @Operation(summary = "Get product details by productId including price, stock, and metadata")
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

    @Operation(summary = "Update product details. Request body: { name, description, price, imageUrls, tags }")
    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("productId", productId, "updated", true));
    }

    @Operation(summary = "Deactivate (soft-delete) a product from the catalog")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> deactivateProduct(@PathVariable String productId) {
        return ResponseEntity.ok(Map.of("productId", productId, "status", "DEACTIVATED"));
    }

    @Operation(summary = "Search products by keyword, category, price range. Query params: q, category, minPrice, maxPrice, inStock, page, size, sortBy")
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

    @Operation(summary = "Update stock quantity for a product. Request body: { quantity, operation: ADD|SUBTRACT|SET }")
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("productId", productId, "newStockQuantity", 300));
    }

    @Operation(summary = "Get stock history and movement log for a product. Query params: fromDate, toDate, page, size")
    @GetMapping("/{productId}/stock/history")
    public ResponseEntity<Map<String, Object>> getStockHistory(
            @PathVariable String productId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(Map.of("productId", productId, "movements", List.of()));
    }

    @Operation(summary = "Add a customer review to a product. Request body: { customerId, rating (1-5), title, body }")
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

    @Operation(summary = "Get all approved reviews for a product. Query params: page, size, sortBy (recent|rating)")
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

    @Operation(summary = "Update product pricing including base price, sale price, and tax category. Request body: { price, salePrice, taxCategory }")
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
