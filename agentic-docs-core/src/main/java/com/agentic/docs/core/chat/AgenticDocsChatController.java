package com.agentic.docs.core.chat;

import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.EndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/** Exposes Agentic Docs HTTP endpoints. Rate limiting is handled by {@link com.agentic.docs.core.ratelimit.RateLimitInterceptor}. */
@RestController
@RequestMapping("/agentic-docs/api")
public class AgenticDocsChatController {

    private static final Logger log = LoggerFactory.getLogger(AgenticDocsChatController.class);

    private final EndpointRepository endpointRepository;
    private final ChatPort chatPort;

    public AgenticDocsChatController(EndpointRepository endpointRepository,
                                     ChatPort chatPort) {
        this.endpointRepository = endpointRepository;
        this.chatPort           = chatPort;
    }

    @GetMapping("/endpoints")
    public ResponseEntity<List<ApiEndpointMetadata>> listEndpoints() {
        return ResponseEntity.ok(endpointRepository.getScannedEndpoints());
    }

    @GetMapping("/chat")
    public ResponseEntity<ChatResponse> chatInfo() {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ChatResponse(
                        "This endpoint only accepts POST requests. " +
                        "Please use the Agentic Docs UI at /agentic-docs/ " +
                        "or POST a JSON body: {\"question\": \"\"}"));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please provide a non-empty question."));
        }
        log.debug("[AgenticDocs] Received chat request.");
        return ResponseEntity.ok(chatPort.answer(request));
    }

    /** Streaming chat via SSE. Events: token, done, error. */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> chatStream(
            @Validated @RequestBody ChatRequest request) {

        Flux<ServerSentEvent<String>> sseFlux = chatPort.streamAnswer(request)
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token").data(token).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done").data("[DONE]").build()))
                .onErrorResume(ex -> {
                    log.error("[AgenticDocs] Streaming error: {}", ex.getMessage(), ex);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("Streaming failed: " + ex.getMessage())
                            .build());
                });

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(sseFlux);
    }
}
