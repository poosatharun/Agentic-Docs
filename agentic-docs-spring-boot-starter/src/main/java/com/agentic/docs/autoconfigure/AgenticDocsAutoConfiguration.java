package com.agentic.docs.autoconfigure;

import com.agentic.docs.core.config.AgenticDocsProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot AutoConfiguration for Agentic Docs.
 *
 * <p>Activated automatically when:
 * <ul>
 *   <li>The application is a Servlet web application (not reactive)</li>
 *   <li>{@code agentic.docs.enabled} is {@code true} (the default — omitting it also activates this)</li>
 * </ul>
 *
 * <p>What this class does:
 * <ol>
 *   <li><strong>@EnableConfigurationProperties</strong> — tells Spring Boot to read all
 *       {@code agentic.docs.*} properties from {@code application.properties} and bind them
 *       into an {@link AgenticDocsProperties} bean available for injection everywhere.</li>
 *   <li><strong>@ComponentScan</strong> — scans the {@code com.agentic.docs.core} package
 *       and registers all {@code @Component}, {@code @Service}, {@code @RestController},
 *       and {@code @Configuration} classes as Spring beans.</li>
 * </ol>
 *
 * <p>Beans registered via the component scan:
 * <ul>
 *   <li>{@code ApiMetadataScanner} — discovers all {@code @RestController} endpoints</li>
 *   <li>{@code VectorStoreConfig} — creates an in-memory {@code SimpleVectorStore} (unless you provide your own)</li>
 *   <li>{@code ApiDocumentIngestor} — embeds endpoint descriptions into the vector store</li>
 *   <li>{@code AgenticDocsChatService} — runs the RAG pipeline and calls the LLM</li>
 *   <li>{@code AgenticDocsChatController} — exposes {@code /agentic-docs/api/**} HTTP endpoints</li>
 *   <li>{@code AgenticDocsMvcConfigurer} — sets up UI forwarding and CORS rules</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "agentic.docs", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgenticDocsProperties.class)  // ← binds agentic.docs.* into AgenticDocsProperties
@ComponentScan(basePackages = "com.agentic.docs.core")
public class AgenticDocsAutoConfiguration {
    // All beans are registered via @ComponentScan + @Configuration/@Component annotations
    // in the core module. No manual @Bean declarations needed here.
}
