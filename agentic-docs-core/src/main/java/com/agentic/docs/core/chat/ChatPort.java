package com.agentic.docs.core.chat;

import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Unified port for the Agentic Docs chat pipeline (Gap 5 — Architecture).
 *
 * <p>Merges the previously separate {@link ChatService} (blocking) and
 * {@link StreamingChatService} (reactive) contracts into a single interface.
 * This eliminates the {@code instanceof} check in the controller and provides
 * every consumer a single, stable dependency:</p>
 *
 * <ul>
 *   <li><strong>Controllers</strong> depend only on {@code ChatPort} — one dependency,
 *       no casting, no {@code instanceof}.</li>
 *   <li><strong>Tests</strong> mock only one interface — no mock tree to maintain.</li>
 *   <li><strong>Custom implementations</strong> (caching, A/B routing, multi-model)
 *       implement one contract instead of two.</li>
 * </ul>
 *
 * <h2>How to provide a custom implementation</h2>
 * <pre>{@code
 * @Bean
 * @Primary
 * public ChatPort myChatPort() {
 *     return new MyCachingChatPort(delegate, cache);
 * }
 * }</pre>
 */
public interface ChatPort {

    /**
     * Answers a developer question using the RAG pipeline (blocking).
     *
     * @param request the incoming chat request containing the user's question
     * @return a {@link ChatResponse} holding the LLM-generated answer
     */
    ChatResponse answer(ChatRequest request);

    /**
     * Streams the LLM answer token-by-token (reactive).
     *
     * <p>Each emitted {@code String} is one raw token from the model. The returned
     * {@link Flux} completes normally when the model finishes, or with an error
     * if the call fails.</p>
     *
     * @param request the incoming chat request containing the user's question
     * @return a {@link Flux} of token strings
     */
    Flux<String> streamAnswer(ChatRequest request);
}
