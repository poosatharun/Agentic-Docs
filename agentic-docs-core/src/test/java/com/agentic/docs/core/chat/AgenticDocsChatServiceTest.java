package com.agentic.docs.core.chat;

import com.agentic.docs.core.config.AgenticDocsProperties;
import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AgenticDocsChatService}.
 *
 * <h2>What is a unit test?</h2>
 * <p>A unit test checks one class in complete isolation. We use <strong>Mockito</strong>
 * to create fake (mock) versions of every dependency ({@code VectorStore}, {@code ChatClient})
 * so we can control exactly what they return — no real database, no real LLM API call needed.</p>
 *
 * <h2>Annotations used</h2>
 * <ul>
 *   <li>{@code @ExtendWith(MockitoExtension.class)} — activates Mockito for this test class</li>
 *   <li>{@code @Mock} — tells Mockito "create a fake object of this type for me"</li>
 *   <li>{@code @BeforeEach} — runs before every @Test method to reset state</li>
 *   <li>{@code @Test} — marks a method as a test case</li>
 *   <li>{@code @DisplayName} — gives the test a readable name in the test report</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AgenticDocsChatServiceTest {

    // ── Mocks (fake dependencies) ─────────────────────────────────────────────

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    // These mock the Spring AI fluent chain: chatClient.prompt().system(...).user(...).call().content()
    @Mock
    private ChatClient.ChatClientRequestSpec promptSpec;


    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    // ── The class under test ──────────────────────────────────────────────────

    private AgenticDocsChatService service;

    @BeforeEach
    void setUp() {
        // Wire the mocks into the fluent builder chain
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Create fresh properties using the record constructor (defaults: enabled=true, topK=5)
        AgenticDocsProperties properties = new AgenticDocsProperties(
                true,
                5,
                null,
                new AgenticDocsProperties.Cors(java.util.List.of("http://localhost:5173"))
        );

        // Build the real service, injecting all fake dependencies
        service = new AgenticDocsChatService(vectorStore, chatClientBuilder, properties);
    }

    // ── Test cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("answer() returns LLM response when vector store finds relevant documents")
    void answer_returnsLlmResponse_whenDocumentsFound() {
        // GIVEN: the vector store returns one relevant document
        Document fakeDoc = new Document("POST /api/users — creates a new user");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(fakeDoc));

        // AND: the LLM returns a real answer
        mockChatClientChain("Use POST /api/users with body {\"name\": \"John\"}");

        // WHEN: we call the service
        ChatResponse response = service.answer(new ChatRequest("How do I create a user?"));

        // THEN: the response contains the LLM's answer
        assertThat(response.answer())
                .isEqualTo("Use POST /api/users with body {\"name\": \"John\"}");
    }

    @Test
    @DisplayName("answer() returns fallback message when LLM returns null")
    void answer_returnsFallback_whenLlmReturnsNull() {
        // GIVEN: vector store finds nothing
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // AND: the LLM returns null (e.g. empty model response)
        mockChatClientChain(null);

        // WHEN: we call the service
        ChatResponse response = service.answer(new ChatRequest("What is the meaning of life?"));

        // THEN: the service returns the fallback message instead of null
        assertThat(response.answer())
                .contains("I could not find a relevant endpoint");
    }

    @Test
    @DisplayName("answer() returns fallback message when LLM returns blank string")
    void answer_returnsFallback_whenLlmReturnsBlank() {
        // GIVEN: vector store finds nothing
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // AND: the LLM returns a blank string
        mockChatClientChain("   ");

        // WHEN: we call the service
        ChatResponse response = service.answer(new ChatRequest("What is the meaning of life?"));

        // THEN: the service returns the fallback message
        assertThat(response.answer())
                .contains("I could not find a relevant endpoint");
    }

    @Test
    @DisplayName("answer() calls vector store with the user's question")
    void answer_callsVectorStore_withUserQuestion() {
        // GIVEN: vector store returns empty list
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        mockChatClientChain("No match found.");

        // WHEN: we ask a specific question
        service.answer(new ChatRequest("How do I delete an account?"));

        // THEN: the vector store was actually called (not skipped)
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("DEFAULT_SYSTEM_PROMPT contains the {context} placeholder")
    void defaultSystemPrompt_containsContextPlaceholder() {
        // This test protects against accidentally removing the {context} placeholder
        // which would break the RAG pipeline silently.
        assertThat(AgenticDocsChatService.DEFAULT_SYSTEM_PROMPT)
                .contains("{context}");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Sets up the Mockito mock chain for the Spring AI ChatClient fluent API.
     *
     * <p>The real call looks like:
     * <pre>
     *   chatClient.prompt()
     *       .system(...)
     *       .user(...)
     *       .call()
     *       .content()
     * </pre>
     * Each step returns the next mock in the chain.
     *
     * @param returnValue what {@code .content()} should return
     */
    @SuppressWarnings("unchecked") // Consumer<PromptSystemSpec> erasure — unavoidable with Mockito any()
    private void mockChatClientChain(String returnValue) {
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.system(any(java.util.function.Consumer.class))).thenAnswer(inv -> promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(returnValue);
    }
}
