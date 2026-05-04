package com.agentic.docs.core.chat;

import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.ApiMetadataScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller that exposes the Agentic Docs HTTP endpoints.
 *
 * <p>This class is intentionally kept <strong>thin</strong> — it only handles
 * HTTP-level concerns:</p>
 * <ul>
 *   <li>Reading the incoming request body</li>
 *   <li>Validating that required fields are present</li>
 *   <li>Calling {@link AgenticDocsChatService} for the actual work</li>
 *   <li>Returning the correct HTTP status code and response body</li>
 * </ul>
 *
 * <p>All business logic (RAG retrieval, prompt building, LLM call) lives in
 * {@link ChatService}. This separation makes both classes easier
 * to read, test, and change independently.</p>
 *
 * <p>Endpoints exposed:</p>
 * <ul>
 *   <li>{@code GET  /agentic-docs/api/endpoints} — list all scanned REST endpoints</li>
 *   <li>{@code GET  /agentic-docs/api/chat}      — returns a friendly usage hint</li>
 *   <li>{@code POST /agentic-docs/api/chat}      — ask a question, receive an AI answer</li>
 * </ul>
 */
@RestController
@RequestMapping("/agentic-docs/api")
public class AgenticDocsChatController {

    private static final Logger log = LoggerFactory.getLogger(AgenticDocsChatController.class);

    // ── Dependencies ──────────────────────────────────────────────────────────

    /** Provides the list of scanned REST endpoints for the API Explorer UI tab. */
    private final ApiMetadataScanner apiMetadataScanner;

    /** Owns all RAG + LLM logic. The controller delegates to this service. */
    private final ChatService chatService;

    /** Single-thread executor for driving SSE streaming off the request thread. */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    /**
     * Spring injects both dependencies automatically via constructor injection.
     * {@code ChatService} is injected by interface — any {@code @Bean} that implements
     * {@link ChatService} will be used, including custom consumer-provided implementations.
     *
     * @param apiMetadataScanner scans and holds the discovered endpoints
     * @param chatService        handles the RAG pipeline and LLM call
     */
    public AgenticDocsChatController(ApiMetadataScanner apiMetadataScanner,
                                     ChatService chatService) {
        this.apiMetadataScanner = apiMetadataScanner;
        this.chatService        = chatService;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * Returns all REST endpoints discovered in the host application.
     * Used by the "API Explorer" tab in the embedded UI.
     *
     * @return 200 OK with a list of {@link ApiEndpointMetadata} objects
     */
    @GetMapping("/endpoints")
    public ResponseEntity<List<ApiEndpointMetadata>> listEndpoints() {
        return ResponseEntity.ok(apiMetadataScanner.getScannedEndpoints());
    }

    /**
     * Friendly hint when someone calls GET /chat by mistake in a browser or curl.
     * Returns 405 Method Not Allowed with a helpful explanation.
     *
     * @return 405 with a usage hint message
     */
    @GetMapping("/chat")
    public ResponseEntity<ChatResponse> chatInfo() {
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ChatResponse(
                        "This endpoint only accepts POST requests. " +
                        "Please use the Agentic Docs UI at /agentic-docs/ " +
                        "or POST a JSON body: {\"question\": \"...\"}"));
    }

    /**
     * Main chat endpoint. Accepts a developer question and returns an AI-generated answer.
     *
     * <p>Request body example:
     * <pre>{@code {"question": "How do I create a new user?"}}</pre>
     *
     * <p>Response body example:
     * <pre>{@code {"answer": "Use POST /api/users with body {\"name\": \"...\"}"}}</pre>
     *
     * @param request the chat request containing the developer's question
     * @return 200 OK with an AI answer, or 400 Bad Request if the question is blank
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
        // @Validated triggers Jakarta Bean Validation on the ChatRequest fields.
        // If @NotBlank or @Size constraints fail, Spring returns 400 automatically.
        // The manual null/blank check below is kept as a readable safety net.
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please provide a non-empty question."));
        }

        log.debug("[AgenticDocs] Received chat request.");

        // Delegate ALL business logic to the service layer
        ChatResponse response = chatService.answer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Streaming chat endpoint — delivers LLM tokens via Server-Sent Events (SSE)
     * as they are generated. This dramatically improves perceived latency for local
     * Ollama models by showing text immediately instead of waiting for the full response.
     *
     * <p>Each SSE event contains one raw token string. A final {@code [DONE]} event
     * signals that the stream has completed.</p>
     *
     * @param request the chat request containing the developer's question
     * @return an SSE stream of token strings
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Please provide a non-empty question."));
            } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        // 3-minute timeout — Ollama on slow hardware may take time on first token
        SseEmitter emitter = new SseEmitter(180_000L);

        streamExecutor.execute(() -> {
            if (!(chatService instanceof StreamingChatService streamingService)) {
                // Fallback: this ChatService implementation does not support streaming
                try {
                    ChatResponse response = chatService.answer(request);
                    emitter.send(SseEmitter.event().name("token").data(response.answer()));
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
                return;
            }

            streamingService.streamAnswer(request)
                    .subscribe(
                            token -> {
                                try {
                                    emitter.send(SseEmitter.event().name("token").data(token));
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            () -> {
                                try {
                                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }
                    );
        });

        return emitter;
    }
}
