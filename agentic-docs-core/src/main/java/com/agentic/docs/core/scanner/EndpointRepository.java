package com.agentic.docs.core.scanner;

import java.util.List;

/**
 * Contract for accessing the list of REST endpoints discovered in the host application.
 *
 * <p>Separating this contract from the concrete {@link ApiMetadataScanner} implementation
 * satisfies the <strong>Dependency Inversion Principle</strong>:
 * higher-level modules (the controller, the ingestor) depend on this abstraction
 * rather than on the scanner's implementation details.</p>
 *
 * <p>It also satisfies the <strong>Open/Closed Principle</strong>:
 * teams can provide a custom {@code @Primary @Bean EndpointRepository} — for example,
 * one that reads endpoints from a YAML file, a remote registry, or an OpenAPI spec —
 * without modifying any existing class.</p>
 *
 * <h2>How to provide a custom implementation</h2>
 * <pre>{@code
 * @Bean
 * @Primary
 * public EndpointRepository myEndpointRepository() {
 *     return () -> List.of(
 *         new ApiEndpointMetadata("/custom/path", "GET", ...)
 *     );
 * }
 * }</pre>
 */
@FunctionalInterface
public interface EndpointRepository {

    /**
     * Returns an immutable snapshot of all discovered REST endpoints.
     *
     * <p>Implementations must guarantee:</p>
     * <ul>
     *   <li>Never returns {@code null} — return an empty list if nothing was found.</li>
     *   <li>The returned list is unmodifiable.</li>
     *   <li>This method is safe to call from multiple threads concurrently.</li>
     * </ul>
     *
     * @return immutable list of discovered endpoints (may be empty, never null)
     */
    List<ApiEndpointMetadata> getScannedEndpoints();
}
