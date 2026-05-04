package com.agentic.docs.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for Agentic Docs (prefix: {@code agentic.docs}).
 * Validated at startup — invalid values cause a clear failure instead of a silent runtime bug.
 *
 * <pre>
 * agentic.docs.enabled=true
 * agentic.docs.top-k=5                          # 1-50
 * agentic.docs.system-prompt=...{context}...    # optional; must contain {context}
 * agentic.docs.vector-store-path=./agentic-docs-vector-store.json
 * agentic.docs.rate-limit.enabled=true
 * agentic.docs.rate-limit.requests-per-minute=20
 * agentic.docs.cors.allowed-origins=http://localhost:5173
 * </pre>
 */
@ConfigurationProperties(prefix = "agentic.docs")
@Validated
public record AgenticDocsProperties(
        @DefaultValue("true")                              boolean   enabled,
        @DefaultValue("5")   @Min(1) @Max(50)             int       topK,
        String                                             systemPrompt,
        @DefaultValue("./agentic-docs-vector-store.json")
        @NotBlank                                          String    vectorStorePath,
        @DefaultValue @Valid                               RateLimit rateLimit,
        @DefaultValue @Valid                               Cors      cors
) {

    /** CORS settings — maps to {@code agentic.docs.cors.*}. */
    public record Cors(
            @DefaultValue("http://localhost:5173") List<String> allowedOrigins
    ) {}

    /** Per-IP rate limit settings — maps to {@code agentic.docs.rate-limit.*}. */
    public record RateLimit(
            @DefaultValue("true")   boolean enabled,
            @DefaultValue("20")     @Min(1) @Max(10000) int requestsPerMinute
    ) {}
}
