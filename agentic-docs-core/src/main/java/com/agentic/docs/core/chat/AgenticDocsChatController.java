package com.agentic.docs.core.chat;

import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.ApiMetadataScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;

/**
 * Handles chat requests from the embedded UI.
 *
 * <p>Flow:
 * <ol>
 *   <li>Receive user question via {@code POST /agentic-docs/api/chat}</li>
 *   <li>Run vector similarity search (top-5) against indexed endpoints</li>
 *   <li>Inject retrieved context into a strict system prompt</li>
 *   <li>Call the configured LLM and stream the answer back</li>
 * </ol>
 */
@RestController
@RequestMapping("/agentic-docs/api")
@CrossOrigin(origins = "*")
public class AgenticDocsChatController {

    private static final Logger log = LoggerFactory.getLogger(AgenticDocsChatController.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert API assistant embedded inside developer documentation.
            Your sole job is to help developers understand and use the REST APIs of THIS application.

            Rules:
            - Answer ONLY using the API context provided below. Do not invent endpoints.
            - When asked for implementation, generate concise, correct Java or React code snippets
              using the exact paths, HTTP methods, and field names from the context.
            - If the answer cannot be derived from the context, say:
              "I could not find a relevant endpoint for that. Please check the Swagger UI."
            - Keep answers focused and developer-friendly.

            API Context:
            ---
            {context}
            ---
            """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ApiMetadataScanner apiMetadataScanner;

    public AgenticDocsChatController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder,
                                     ApiMetadataScanner apiMetadataScanner) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.apiMetadataScanner = apiMetadataScanner;
    }

    @GetMapping("/endpoints")
    public ResponseEntity<List<ApiEndpointMetadata>> listEndpoints() {
        return ResponseEntity.ok(apiMetadataScanner.getScannedEndpoints());
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please provide a non-empty question."));
        }
        log.debug("[AgenticDocs] Received question: {}", request.question());

        // 1. Retrieve the top-5 most relevant endpoint documents
        List<Document> relevant = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.question())
                        .topK(5)
                        .build()
        );

        String context = relevant.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 2. Call the LLM with the retrieved context injected into the system prompt
        String answer = chatClient.prompt()
                .system(s -> s.text(SYSTEM_PROMPT).param("context", context))
                .user(request.question())
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            answer = "I could not find a relevant endpoint for that. Please check the Swagger UI.";
        }

        return ResponseEntity.ok(new ChatResponse(answer));
    }

    // ── Request / Response records ────────────────────────────────────────────

    public record ChatRequest(String question) {}
    public record ChatResponse(String answer) {}
}
