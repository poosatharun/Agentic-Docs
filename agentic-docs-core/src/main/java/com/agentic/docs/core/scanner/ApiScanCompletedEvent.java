package com.agentic.docs.core.scanner;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Domain event published by {@link ApiMetadataScanner} after endpoint discovery finishes
 * (Gap 4 — Architecture).
 *
 * <p>Using an explicit domain event instead of listening on Spring's generic
 * {@code ContextRefreshedEvent} provides three concrete benefits:</p>
 *
 * <ol>
 *   <li><strong>Explicitness</strong> — the causal relationship is expressed in code:
 *       "scanning completed" triggers "ingestion starts", not a Spring lifecycle detail.</li>
 *   <li><strong>No {@code @Order} coupling</strong> — the scanner and ingestor are
 *       decoupled from each other's bean initialization order. The ingestor starts
 *       exactly when the scanner signals readiness.</li>
 *   <li><strong>Testability</strong> — tests can fire this event directly on the
 *       ingestor with a hand-crafted endpoint list, requiring no Spring context and
 *       no {@code ContextRefreshedEvent} semantics.</li>
 * </ol>
 */
public class ApiScanCompletedEvent extends ApplicationEvent {

    private final List<ApiEndpointMetadata> endpoints;

    /**
     * Creates the event.
     *
     * @param source    the bean that published the event (typically {@code ApiMetadataScanner})
     * @param endpoints the immutable list of endpoints just discovered
     */
    public ApiScanCompletedEvent(Object source, List<ApiEndpointMetadata> endpoints) {
        super(source);
        this.endpoints = List.copyOf(endpoints); // defensive copy — always immutable
    }

    /**
     * Returns the endpoints discovered during the scan.
     * The list is immutable; endpoints are already filtered and validated.
     */
    public List<ApiEndpointMetadata> endpoints() {
        return endpoints;
    }
}
