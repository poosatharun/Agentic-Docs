package com.agentic.docs.core.infrastructure;

import com.agentic.docs.core.port.VectorStorePort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapts Spring AI's {@link VectorStore} to the {@link VectorStorePort} interface.
 * To swap vector store backends (SimpleVectorStore → PGVector, Redis, etc.),
 * write a new {@code @Primary} {@link VectorStorePort} implementation.
 */
@Component
public class VectorStoreAdapter implements VectorStorePort {

    private final VectorStore vectorStore;

    public VectorStoreAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<String> findRelevantContext(String question, int topK) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .build()
        );
        return docs.stream()
                .map(Document::getText)
                .toList();
    }
}
