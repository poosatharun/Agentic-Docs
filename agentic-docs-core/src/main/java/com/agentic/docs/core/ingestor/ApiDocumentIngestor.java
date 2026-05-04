package com.agentic.docs.core.ingestor;

import com.agentic.docs.core.config.AgenticDocsProperties;
import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.ApiScanCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listens for {@link ApiScanCompletedEvent} and ingests discovered endpoints
 * into the {@link VectorStore} as embeddings for RAG similarity search.
 * Skips ingest if the vector store file already exists on disk.
 */
@Component
public class ApiDocumentIngestor {

    private static final Logger log = LoggerFactory.getLogger(ApiDocumentIngestor.class);

    private final VectorStore vectorStore;
    private final AgenticDocsProperties properties;
    private final AtomicBoolean ingested = new AtomicBoolean(false);

    public ApiDocumentIngestor(VectorStore vectorStore,
                                AgenticDocsProperties properties) {
        this.vectorStore = vectorStore;
        this.properties  = properties;
    }

    @EventListener
    public void onScanCompleted(ApiScanCompletedEvent event) {
        if (!ingested.compareAndSet(false, true)) return;

        if (new File(properties.vectorStorePath()).exists()) {
            log.info("[AgenticDocs] Vector store found on disk — skipping ingest.");
            return;
        }

        List<ApiEndpointMetadata> endpoints = event.endpoints();
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

        try {
            vectorStore.add(documents);
            log.info("[AgenticDocs] Ingested {} endpoint documents into the vector store.", documents.size());
        } catch (Exception ex) {
            log.warn("[AgenticDocs] Vector store ingestion failed ({}). "
                    + "API Explorer still works; only AI chat results may be affected.",
                    ex.getMessage());
        }
    }
}
