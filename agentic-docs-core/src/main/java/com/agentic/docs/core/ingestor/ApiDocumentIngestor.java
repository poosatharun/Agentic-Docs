package com.agentic.docs.core.ingestor;

import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.ApiMetadataScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs once after the application context is fully refreshed.
 * Converts every {@link ApiEndpointMetadata} into a Spring AI {@link Document}
 * and stores it in the {@link VectorStore} for similarity search.
 *
 * <p>{@code @Order(1)} ensures this runs after {@link ApiMetadataScanner}
 * (which also listens on {@link ContextRefreshedEvent} with default order).</p>
 */
@Component
public class ApiDocumentIngestor {

    private static final Logger log = LoggerFactory.getLogger(ApiDocumentIngestor.class);

    private final ApiMetadataScanner scanner;
    private final VectorStore vectorStore;
    private final AtomicBoolean ingested = new AtomicBoolean(false);

    public ApiDocumentIngestor(ApiMetadataScanner scanner, VectorStore vectorStore) {
        this.scanner = scanner;
        this.vectorStore = vectorStore;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order(1)
    public void ingest() {
        // Guard against duplicate events (parent/child context refreshes)
        if (!ingested.compareAndSet(false, true)) return;

        List<ApiEndpointMetadata> endpoints = scanner.getScannedEndpoints();
        if (endpoints.isEmpty()) {
            log.warn("[AgenticDocs] No endpoints found to ingest. Is the host app a @SpringBootApplication?");
            return;
        }

        List<Document> documents = endpoints.stream()
                .map(e -> new Document(
                        e.toLlmReadableText(),
                        Map.of(
                                "path",       e.path(),
                                "httpMethod", e.httpMethod(),
                                "controller", e.controllerName(),
                                "method",     e.methodName()
                        )
                ))
                .toList();

        vectorStore.add(documents);
        log.info("[AgenticDocs] Ingested {} endpoint documents into the vector store.", documents.size());
    }
}
