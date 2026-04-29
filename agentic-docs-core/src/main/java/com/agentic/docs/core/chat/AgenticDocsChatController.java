package com.agentic.docs.core.chat;

import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.ApiMetadataScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
