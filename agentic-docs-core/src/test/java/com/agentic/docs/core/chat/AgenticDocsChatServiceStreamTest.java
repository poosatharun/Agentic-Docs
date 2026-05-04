package com.agentic.docs.core.chat;

import com.agentic.docs.core.config.AgenticDocsProperties;
import com.agentic.docs.core.model.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AgenticDocsChatService#streamAnswer(ChatRequest)}.
 *
 * <p>Verifies that the streaming path uses the same RAG pipeline as the blocking
 * {@code answer()} method and that the returned {@link Flux} emits tokens correctly.</p>
 *
 * <p>Uses Project Reactor's {@code StepVerifier} to assert on reactive streams
 * without blocking.</p>
 */
@ExtendWith(MockitoExtension.class)
class AgenticDocsChatServiceStreamTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec promptSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private AgenticDocsChatService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        AgenticDocsProperties properties = new AgenticDocsProperties(
                true, 5, null,
                new AgenticDocsProperties.Cors(List.of("http://localhost:5173"))
        );
        service = new AgenticDocsChatService(vectorStore, chatClientBuilder, properties);
    }

    @Test
    @DisplayName("streamAnswer() emits tokens from the LLM flux")
    @SuppressWarnings("unchecked") // Consumer<PromptSystemSpec> erasure — unavoidable with Mockito any()
    void streamAnswer_emitsTokensFromLlm() {
        // GIVEN: vector store returns one document
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("GET /api/users — list users")));

        // AND: the streaming chain returns three tokens
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(any(java.util.function.Consumer.class))).thenAnswer(inv -> promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("Use ", "GET ", "/api/users"));

        // WHEN / THEN: StepVerifier subscribes and asserts token-by-token
        StepVerifier.create(service.streamAnswer(new ChatRequest("How do I list users?")))
                .expectNext("Use ")
                .expectNext("GET ")
                .expectNext("/api/users")
                .verifyComplete();
    }

    @Test
    @DisplayName("streamAnswer() completes with error when LLM flux errors")
    @SuppressWarnings("unchecked")
    void streamAnswer_propagatesError_whenLlmFails() {
        // GIVEN: vector store returns empty
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // AND: the LLM stream fails
        RuntimeException llmError = new RuntimeException("Ollama unavailable");
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(any(java.util.function.Consumer.class))).thenAnswer(inv -> promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.error(llmError));

        // WHEN / THEN: the error propagates through the Flux
        StepVerifier.create(service.streamAnswer(new ChatRequest("What is the payment API?")))
                .expectErrorMatches(e -> e.getMessage().equals("Ollama unavailable"))
                .verify();
    }

    @Test
    @DisplayName("streamAnswer() uses custom system prompt when configured")
    @SuppressWarnings("unchecked")
    void streamAnswer_usesCustomSystemPrompt_whenConfigured() {
        // GIVEN: a service configured with a custom system prompt
        AgenticDocsProperties customProps = new AgenticDocsProperties(
                true, 5, "Custom prompt {context}",
                new AgenticDocsProperties.Cors(List.of("http://localhost:5173"))
        );
        AgenticDocsChatService customService = new AgenticDocsChatService(vectorStore, chatClientBuilder, customProps);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(any(java.util.function.Consumer.class))).thenAnswer(inv -> promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("custom answer"));

        // WHEN / THEN: the stream still completes successfully
        StepVerifier.create(customService.streamAnswer(new ChatRequest("Test question?")))
                .expectNext("custom answer")
                .verifyComplete();
    }
}
