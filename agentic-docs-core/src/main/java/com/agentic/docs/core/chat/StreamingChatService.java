package com.agentic.docs.core.chat;

import com.agentic.docs.core.model.ChatRequest;
import reactor.core.publisher.Flux;

/**
 * Optional extension of {@link ChatService} for implementations that support
 * token-by-token streaming via a reactive {@link Flux}.
 *
 * <p>Separating the streaming contract into its own interface preserves the
 * SOLID principles that the base {@link ChatService} establishes:</p>
 *
 * <ul>
 *   <li><strong>ISP</strong> — basic consumers only depend on {@link ChatService#answer};
 *       streaming is an opt-in capability, not forced on every implementor.</li>
 *   <li><strong>OCP</strong> — {@link AgenticDocsChatController} checks for
 *       {@code StreamingChatService} via {@code instanceof}, so new streaming
 *       implementations are picked up automatically without modifying the controller.</li>
 *   <li><strong>LSP</strong> — any class that declares {@code implements StreamingChatService}
 *       is guaranteed to provide a real streaming path; the controller never silently
 *       downgrades a streaming-capable service to the blocking fallback.</li>
 * </ul>
 *
 * <h2>How to provide a custom streaming implementation</h2>
 * <pre>{@code
 * @Bean
 * @Primary
 * public StreamingChatService myStreamingService() {
 *     return new MyCustomStreamingChatService(...);
 * }
 * }</pre>
 */
public interface StreamingChatService extends ChatService {

    /**
     * Streams the LLM answer token-by-token.
     *
     * <p>Each emitted {@code String} is one raw token from the model. The returned
     * {@link Flux} completes normally when the model finishes generating, or with
     * an error if the call fails.</p>
     *
     * @param request the incoming chat request containing the user's question
     * @return a {@link Flux} of token strings
     */
    Flux<String> streamAnswer(ChatRequest request);
}
