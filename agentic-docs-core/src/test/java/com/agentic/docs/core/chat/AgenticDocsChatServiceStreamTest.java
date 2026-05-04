package com.agentic.docs.core.chat;

import com.agentic.docs.core.config.AgenticDocsProperties;
import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.port.LlmPort;
import com.agentic.docs.core.port.VectorStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenticDocsChatServiceStreamTest {

    @Mock private VectorStorePort vectorStorePort;
    @Mock private LlmPort llmPort;

    private AgenticDocsChatService service;

    @BeforeEach
    void setUp() {
        AgenticDocsProperties props = new AgenticDocsProperties(
                true, 5, null,
                "./agentic-docs-vector-store.json",
                new AgenticDocsProperties.RateLimit(true, 20),
                new AgenticDocsProperties.Cors(List.of("http://localhost:5173"))
        );
        service = new AgenticDocsChatService(vectorStorePort, llmPort, props);
    }

    @Test
    @DisplayName("streamAnswer() emits tokens from the LLM flux")
    void streamAnswer_emitsTokensFromLlm() {
        when(vectorStorePort.findRelevantContext(anyString(), anyInt()))
                .thenReturn(List.of("GET /api/users - list users"));
        when(llmPort.stream(anyString(), anyString(), anyString()))
                .thenReturn(Flux.just("Use ", "GET ", "/api/users"));

        StepVerifier.create(service.streamAnswer(new ChatRequest("How do I list users?")))
                .expectNext("Use ")
                .expectNext("GET ")
                .expectNext("/api/users")
                .verifyComplete();
    }

    @Test
    @DisplayName("streamAnswer() propagates error when LLM flux errors")
    void streamAnswer_propagatesError_whenLlmFails() {
        when(vectorStorePort.findRelevantContext(anyString(), anyInt())).thenReturn(List.of());
        when(llmPort.stream(anyString(), anyString(), anyString()))
                .thenReturn(Flux.error(new RuntimeException("Ollama unavailable")));

        StepVerifier.create(service.streamAnswer(new ChatRequest("Anything?")))
                .expectErrorMessage("Ollama unavailable")
                .verify();
    }

    @Test
    @DisplayName("streamAnswer() completes when LLM returns empty flux")
    void streamAnswer_completesEmpty_whenLlmReturnsEmpty() {
        when(vectorStorePort.findRelevantContext(anyString(), anyInt())).thenReturn(List.of());
        when(llmPort.stream(anyString(), anyString(), anyString())).thenReturn(Flux.empty());

        StepVerifier.create(service.streamAnswer(new ChatRequest("Anything?")))
                .verifyComplete();
    }
}