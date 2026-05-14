package com.apiscope.autoconfigure;

import com.apiscope.core.config.AgenticDocsProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot AutoConfiguration for APIScope.
 *
 * Activated automatically when:
 * - The application is a Servlet web application (not reactive)
 * - {@code apiscope.enabled=true} (the default — omitting it also activates this)
 *
 * Beans registered via component scan:
 * - {@code ApiMetadataScanner}      — discovers all @RestController endpoints
 * - {@code VectorStoreConfig}       — creates SimpleVectorStore (only when EmbeddingModel is present)
 * - {@code ApiDocumentIngestor}     — embeds endpoint descriptions into the vector store
 * - {@code AgenticDocsChatService}  — runs the RAG pipeline and calls the LLM
 * - {@code AgenticDocsChatController} — exposes /apiscope/api/** HTTP endpoints
 * - {@code AgenticDocsMvcConfigurer}  — sets up UI forwarding and CORS rules
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "apiscope", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgenticDocsProperties.class)
@ComponentScan(basePackages = "com.apiscope.core")
public class AgenticDocsAutoConfiguration {
    // All beans are registered via @ComponentScan + @Configuration/@Component in the core module.
}
