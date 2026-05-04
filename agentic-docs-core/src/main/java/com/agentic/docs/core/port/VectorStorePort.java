package com.agentic.docs.core.port;

import java.util.List;

/**
 * Domain port for context retrieval from the vector store (Gap 3 — Architecture).
 *
 * <p>This interface lives in the domain layer and contains <strong>zero Spring AI
 * or infrastructure imports</strong>. The application service ({@code AgenticDocsChatService})
 * depends on this port rather than on Spring AI's {@code VectorStore} directly.</p>
 *
 * <h2>Why a port here?</h2>
 * <ul>
 *   <li><strong>Testability</strong> — the service is unit-testable with a plain
 *       lambda or Mockito mock. No Spring AI or embedding model on the test classpath.</li>
 *   <li><strong>Replaceability</strong> — swapping the vector store backend
 *       (SimpleVectorStore → PGVector → Chroma) requires writing a new adapter only;
 *       the service is untouched.</li>
 *   <li><strong>Domain clarity</strong> — the service expresses its intent
 *       ("find relevant context") not its implementation ("run a cosine similarity search
 *       with a SearchRequest DTO").</li>
 * </ul>
 *
 * <p>Implemented by
 * {@link com.agentic.docs.core.infrastructure.VectorStoreAdapter}.</p>
 */
public interface VectorStorePort {

    /**
     * Retrieves the most semantically relevant text chunks for a given question.
     *
     * @param question the natural-language question from the user (already sanitized)
     * @param topK     maximum number of results to return
     * @return list of relevant text chunks joined from matched documents; never null
     */
    List<String> findRelevantContext(String question, int topK);
}
