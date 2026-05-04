package com.agentic.docs.core.port;

import reactor.core.publisher.Flux;

/**
 * Domain port for LLM interaction (Gap 3 — Architecture).
 *
 * <p>Isolates the application service from Spring AI's {@code ChatClient} API.
 * The service calls this port with domain-level primitives (Strings and Flux);
 * the infrastructure adapter translates to and from Spring AI types.</p>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><strong>Testability</strong> — mocking {@code LlmPort} requires no Spring AI
 *       fluent-chain setup ({@code chatClient.prompt().system(...).user(...).call().content()}).
 *       A single {@code when(llmPort.complete(...)).thenReturn("answer")} suffices.</li>
 *   <li><strong>Provider agnosticism</strong> — swapping from Ollama to OpenAI to
 *       Anthropic requires writing a new adapter, not touching the service.</li>
 * </ul>
 *
 * <p>Implemented by
 * {@link com.agentic.docs.core.infrastructure.LlmAdapter}.</p>
 */
public interface LlmPort {

    /**
     * Sends a complete prompt to the LLM and waits for the full response (blocking).
     *
     * @param systemPromptTemplate the system instructions template; must contain
     *                             a {@code {context}} placeholder that will be filled
     *                             by the adapter with the {@code context} argument
     * @param context              the retrieved API documentation context
     * @param question             the sanitized user question
     * @return the LLM-generated answer, or {@code null} if the model returned nothing
     */
    String complete(String systemPromptTemplate, String context, String question);

    /**
     * Streams the LLM response token-by-token (reactive / non-blocking).
     *
     * @param systemPromptTemplate the system instructions template with {@code {context}}
     * @param context              the retrieved API documentation context
     * @param question             the sanitized user question
     * @return a {@link Flux} of token strings that completes when the model finishes
     */
    Flux<String> stream(String systemPromptTemplate, String context, String question);
}
