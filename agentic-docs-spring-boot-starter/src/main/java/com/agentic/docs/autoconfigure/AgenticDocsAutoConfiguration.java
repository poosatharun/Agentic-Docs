package com.agentic.docs.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot AutoConfiguration for Agentic Docs.
 *
 * <p>Activated when:
 * <ul>
 *   <li>The application is a Servlet web application</li>
 *   <li>{@code agentic.docs.enabled=true} is set in application properties</li>
 * </ul>
 *
 * <p>Scans {@code com.agentic.docs.core} to register:
 * <ul>
 *   <li>{@code ApiMetadataScanner} — discovers REST endpoints</li>
 *   <li>{@code VectorStoreConfig} — creates the in-memory SimpleVectorStore</li>
 *   <li>{@code ApiDocumentIngestor} — embeds endpoints into the vector store</li>
 *   <li>{@code AgenticDocsChatController} — serves the /agentic-docs/api/chat endpoint</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "agentic.docs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.agentic.docs.core")
public class AgenticDocsAutoConfiguration {
    // All beans are registered via @ComponentScan + @Configuration/@Component annotations
    // in the core module. No manual @Bean declarations needed here.
}
