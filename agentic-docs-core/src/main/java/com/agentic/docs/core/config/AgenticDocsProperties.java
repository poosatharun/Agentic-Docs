package com.agentic.docs.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * All configurable settings for Agentic Docs, bound from {@code application.properties}
 * (or {@code application.yml}) using the prefix {@code agentic.docs}.
 *
 * <p>This is a Java {@code record} — an immutable, constructor-bound configuration object.
 * Spring Boot 3.2+ supports {@code @ConfigurationProperties} on records natively,
 * eliminating manual getters and setters.</p>
 *
 * <h2>How to configure in application.properties</h2>
 * <pre>
 * # Enable or disable Agentic Docs entirely (default: true)
 * agentic.docs.enabled=true
 *
 * # Number of API endpoint documents fetched per question (default: 5)
 * agentic.docs.top-k=5
 *
 * # Replace the built-in system prompt with your own (must include {context})
 * agentic.docs.system-prompt=You are a helpful API assistant. Context: {context}
 *
 * # CORS: which origins may call /agentic-docs/api/** (default: Vite dev server)
 * agentic.docs.cors.allowed-origins=http://localhost:5173,https://yourapp.com
 * </pre>
 *
 * @param enabled      master switch — set {@code false} to disable entirely (default: {@code true})
 * @param topK         number of vector-store results per question (default: {@code 5})
 * @param systemPrompt custom LLM system prompt; must contain {@code {context}} placeholder (optional)
 * @param cors         CORS settings for {@code /agentic-docs/api/**}
 */
@ConfigurationProperties(prefix = "agentic.docs")
public record AgenticDocsProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("5")    int     topK,
        String systemPrompt,
        @DefaultValue         Cors    cors
) {

    /**
     * CORS configuration. Maps to the {@code agentic.docs.cors.*} prefix.
     *
     * @param allowedOrigins list of origins allowed to call the Agentic Docs API
     *                       (default: {@code http://localhost:5173} — the Vite dev server)
     */
    public record Cors(
            @DefaultValue("http://localhost:5173") List<String> allowedOrigins
    ) {}
}
