package com.agentic.docs.core.chat;

import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;

/**
 * Contract for the Agentic Docs RAG chat pipeline.
 *
 * <p>Defining the service as an interface (rather than injecting the concrete
 * {@link AgenticDocsChatService} directly) provides three key benefits:</p>
 *
 * <ol>
 *   <li><strong>Testability</strong> — controllers can be unit-tested with a simple
 *       lambda or anonymous class instead of a full Mockito mock tree.</li>
 *   <li><strong>Extensibility</strong> — library consumers can provide their own
 *       {@code @Primary @Bean} implementation (e.g. with streaming, caching, or routing)
 *       and Spring will automatically prefer it over the default.</li>
 *   <li><strong>Separation of concerns</strong> — the HTTP layer ({@link AgenticDocsChatController})
 *       depends only on this contract, not on any AI infrastructure detail.</li>
 * </ol>
 *
 * <h2>How to replace the default implementation</h2>
 * <pre>{@code
 * @Bean
 * @Primary
 * public ChatService myChatService() {
 *     return request -> new ChatResponse("Custom answer for: " + request.question());
 * }
 * }</pre>
 */
public interface ChatService {

    /**
     * Answers a developer question using the RAG pipeline.
     *
     * @param request the incoming chat request containing the user's question
     * @return a {@link ChatResponse} holding the LLM-generated answer
     */
    ChatResponse answer(ChatRequest request);
}
