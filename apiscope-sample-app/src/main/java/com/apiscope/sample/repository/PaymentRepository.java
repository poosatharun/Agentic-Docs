package com.apiscope.sample.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

/**
 * Simulated payment repository.
 * Represents the persistence layer for payment records — shown as REPOSITORY
 * in the Flow Tracer execution diagram.
 */
@Repository
public class PaymentRepository {

    /**
     * Validate a payment method (card / wallet) and check it is not expired or blocked.
     *
     * @param paymentMethodId identifier of the stored payment method
     * @return validation result with masked card details
     */
    public Map<String, Object> validatePaymentMethod(String paymentMethodId) {
        simulateLatency(25);
        return Map.of(
                "paymentMethodId", paymentMethodId,
                "valid",           true,
                "type",            "CREDIT_CARD",
                "maskedNumber",    "**** **** **** 4242",
                "expiryMonth",     12,
                "expiryYear",      2028
        );
    }

    /**
     * Persist a new payment record in PENDING state.
     *
     * @param orderId the order this payment belongs to
     * @param amount  the amount to charge in the order's currency
     * @return created payment record
     */
    public Map<String, Object> createPaymentRecord(String orderId, double amount) {
        simulateLatency(18);
        return Map.of(
                "paymentId",  "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "orderId",    orderId,
                "amount",     amount,
                "currency",   "USD",
                "status",     "PENDING",
                "createdAt",  java.time.Instant.now().toString()
        );
    }

    private void simulateLatency(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
