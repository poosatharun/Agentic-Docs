package com.agentic.docs.core.chat;

import com.agentic.docs.core.config.AgenticDocsProperties;
import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic for the Agentic Docs RAG (Retrieval-Augmented Generation) pipeline.
 *
 * <p>This service follows a simple 3-step flow every time a developer asks a question:</p>
 *
 * <pre>
 *  Step 1: Search the vector store for the most relevant API endpoint documents.
 *  Step 2: Combine those documents into a single "context" text block.
 *  Step 3: Send the context + the question to the LLM and return the answer.
 * </pre>
 *
 * <p>Why a separate service?
 * The controller ({@link AgenticDocsChatController}) only handles HTTP concerns
 * (reading the request body, returning the right HTTP status code).
 * This class handles <em>what to do</em> with the data — keeping each class
 * focused on one responsibility (Single Responsibility Principle).</p>
 */
@Service
public class AgenticDocsChatService implements StreamingChatService {

    private static final Logger log = LoggerFactory.getLogger(AgenticDocsChatService.class);

    /**
     * The built-in system prompt used when the user has not configured a custom one.
     *
     * <p>{@code {context}} is a placeholder that Spring AI replaces at runtime
     * with the actual API endpoint text retrieved from the vector store.</p>
     *
     * <p>You can override this entirely by setting
     * {@code agentic.docs.system-prompt=<your prompt>} in {@code application.properties}.
     * Your custom prompt must also contain the {@code {context}} placeholder.</p>
     */
    static final String DEFAULT_SYSTEM_PROMPT = """
            You are an expert API assistant embedded inside developer documentation.
            Your sole job is to help developers understand and use the REST APIs of THIS application.

            Rules:
            - Answer ONLY using the API context provided below. Do not invent endpoints.
            - When asked for implementation, generate concise, correct Java or React code snippets
              using the exact paths, HTTP methods, and field names from the context.
            - If the answer cannot be derived from the context, say:
              "I could not find a relevant endpoint for that. Please check the API Explorer tab."
            - Keep answers focused and developer-friendly.

            API Context:
            ---
            {context}
            ---
            """;

    // ── Dependencies injected by Spring ──────────────────────────────────────

    /** Stores and searches embedded API endpoint documents. */
    private final VectorStore vectorStore;

    /** Spring AI chat client — talks to the configured Ollama model. */
    private final ChatClient chatClient;

    /** Holds all configurable settings (topK, systemPrompt, etc.) from application.properties. */
    private final AgenticDocsProperties properties;

    /**
     * Spring automatically calls this constructor and provides the dependencies.
     *
     * @param vectorStore       the vector store bean (SimpleVectorStore by default)
     * @param chatClientBuilder Spring AI builder — we call {@code .build()} to get the client
     * @param properties        configurable settings from {@code application.properties}
     */
    public AgenticDocsChatService(VectorStore vectorStore,
                                   ChatClient.Builder chatClientBuilder,
                                   AgenticDocsProperties properties) {
        this.vectorStore = vectorStore;
        this.chatClient  = chatClientBuilder.build();
        this.properties  = properties;
    }

    /**
     * Answers a developer's question using the RAG pipeline.
     *
     * @param request contains the natural-language question
     * @return a {@link ChatResponse} with the LLM-generated answer
     */
    public ChatResponse answer(ChatRequest request) {
        log.debug("[AgenticDocs] Processing question: {}", request.question());

        // ── Step 1: Retrieve the most relevant endpoint documents ─────────
        // The vector store turns the question into an embedding vector and
        // finds the top-K most similar endpoint documents.
        // topK is configurable via: agentic.docs.top-k=5
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.question())
                        .topK(properties.topK())
                        .build()
        );

        // ── Step 2: Build the context string ──────────────────────────────
        // Join all retrieved document texts with a separator so the LLM
        // receives a clean, readable block of API information.
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        log.debug("[AgenticDocs] Retrieved {} relevant documents for RAG context.", relevantDocs.size());

        // ── Step 3: Call the LLM ──────────────────────────────────────────
        // Use the user-configured prompt if provided, otherwise fall back to the default.
        // Configurable via: agentic.docs.system-prompt=<your prompt>
        String systemPrompt = (properties.systemPrompt() != null && !properties.systemPrompt().isBlank())
                ? properties.systemPrompt()
                : DEFAULT_SYSTEM_PROMPT;

        String answer = chatClient.prompt()
                .system(s -> s.text(systemPrompt).param("context", context))
                .user(request.question())
                .call()
                .content();

        // Guard against a blank/null response from the LLM
        if (answer == null || answer.isBlank()) {
            answer = "I could not find a relevant endpoint for that. Please check the API Explorer tab.";
        }

        return new ChatResponse(answer);
    }

    /**
     * Streams the LLM answer token-by-token using Spring AI's reactive streaming API.
     *
     * <p>This is used by the {@code POST /agentic-docs/api/chat/stream} SSE endpoint
     * to deliver tokens to the UI as they are generated, dramatically reducing
     * perceived latency when using local models like Ollama.</p>
     *
     * @param request contains the natural-language question
     * @return a {@link Flux} of token strings that complete when the LLM finishes
     */
    public Flux<String> streamAnswer(ChatRequest request) {
        log.debug("[AgenticDocs] Streaming question: {}", request.question());

        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.question())
                        .topK(properties.topK())
                        .build()
        );

        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String systemPrompt = (properties.systemPrompt() != null && !properties.systemPrompt().isBlank())
                ? properties.systemPrompt()
                : DEFAULT_SYSTEM_PROMPT;

        return chatClient.prompt()
                .system(s -> s.text(systemPrompt).param("context", context))
                .user(request.question())
                .stream()
                .content();
    }
}
