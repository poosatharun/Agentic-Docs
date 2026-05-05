package com.agentic.docs.core.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Provides a file-backed {@link SimpleVectorStore} as a fallback (no external DB required).
 * Loads saved embeddings from disk on startup and saves them back on shutdown.
 * Skipped automatically if another {@link VectorStore} bean is present.
 * Configure the path via {@code agentic.docs.vector-store-path}.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    private final ObjectProvider<SimpleVectorStore> storeProvider;
    private final AgenticDocsProperties properties;

    public VectorStoreConfig(ObjectProvider<SimpleVectorStore> storeProvider,
                             AgenticDocsProperties properties) {
        this.storeProvider = storeProvider;
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        File storeFile = new File(properties.vectorStorePath());
        if (storeFile.exists()) {
            store.load(storeFile);
            log.info("[AgenticDocs] Vector store loaded from disk: {}", storeFile.getAbsolutePath());
        } else {
            log.info("[AgenticDocs] No vector store file at '{}' — will ingest on first startup.", storeFile.getAbsolutePath());
        }
        return store;
    }

    @PreDestroy
    public void saveOnShutdown() {
        SimpleVectorStore store = storeProvider.getIfAvailable();
        if (store == null) return;

        File storeFile = new File(properties.vectorStorePath());
        try {
            store.save(storeFile);
            log.info("[AgenticDocs] Vector store saved to disk: {}", storeFile.getAbsolutePath());
        } catch (Exception ex) {
            log.warn("[AgenticDocs] Could not save vector store to disk: {}", ex.getMessage());
        }
    }
}
