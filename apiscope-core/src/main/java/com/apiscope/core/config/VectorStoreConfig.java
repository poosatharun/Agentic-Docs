package com.apiscope.core.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Provides a file-backed {@link SimpleVectorStore} when no other {@link VectorStore} is present.
 * Loads saved embeddings from disk on startup and saves them back on shutdown.
 * Configure the path via {@code apiscope.vector-store-path}.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    private final AgenticDocsProperties properties;
    private SimpleVectorStore store;

    public VectorStoreConfig(AgenticDocsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    @ConditionalOnMissingBean(VectorStore.class)
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        store = SimpleVectorStore.builder(embeddingModel).build();
        File storeFile = new File(properties.vectorStorePath());
        if (storeFile.exists()) {
            store.load(storeFile);
            log.info("[APIScope] Vector store loaded from disk: {}", storeFile.getAbsolutePath());
        } else {
            log.info("[APIScope] No vector store file at '{}' — will ingest on first startup.", storeFile.getAbsolutePath());
        }
        return store;
    }

    @PreDestroy
    public void saveOnShutdown() {
        if (store == null) return;
        File storeFile = new File(properties.vectorStorePath());
        try {
            store.save(storeFile);
            log.info("[APIScope] Vector store saved to disk: {}", storeFile.getAbsolutePath());
        } catch (Exception ex) {
            log.warn("[APIScope] Could not save vector store to disk: {}", ex.getMessage());
        }
    }
}
