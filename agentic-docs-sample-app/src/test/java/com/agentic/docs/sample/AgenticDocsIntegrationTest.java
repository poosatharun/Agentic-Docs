package com.agentic.docs.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test that starts a full Spring Boot application context and verifies
 * that the Agentic Docs starter wires up correctly end-to-end.
 *
 * <h2>Why this test matters</h2>
 * <p>Unit tests (with Mockito) check individual classes in isolation. This integration
 * test checks that all the pieces connect correctly — auto-configuration, component
 * scanning, MVC routing, CORS configuration, and Bean Validation all fire in the right
 * order when a real Spring context starts.</p>
 *
 * <h2>How LLM dependencies are handled</h2>
 * <p>{@code @MockBean} replaces the real {@link VectorStore} and {@link ChatClient.Builder}
 * beans with no-op mocks <em>before</em> the context is fully started.
 * This means no real LLM API key or Ollama instance is needed to run these tests —
 * they can run in CI with zero external dependencies.</p>
 *
 * <h2>Key scenarios tested</h2>
 * <ul>
 *   <li>The auto-configuration registers the endpoints controller correctly.</li>
 *   <li>The endpoints listing API returns HTTP 200 with a JSON array.</li>
 *   <li>Bean Validation rejects blank questions with HTTP 400.</li>
 *   <li>GET on the chat endpoint returns HTTP 405 (POST-only).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Use ollama profile but mock VectorStore and ChatClient.Builder so no
        // real Ollama instance is required during CI.
        "spring.profiles.active=ollama"
})
class AgenticDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Replaced with a mock so no EmbeddingModel or real vector DB is needed.
     * {@code VectorStoreConfig} is {@code @ConditionalOnMissingBean}, so this mock
     * takes precedence and the real {@code SimpleVectorStore} is never created.
     */
    @MockBean
    private VectorStore vectorStore;

    /**
     * Replaced with a mock so no real LLM client is wired up.
     * The POST /chat test never actually reaches the LLM — it's rejected at the
     * Bean Validation layer (blank question) before the service is called.
     */
    @MockBean
    private ChatClient.Builder chatClientBuilder;

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /agentic-docs/api/endpoints — returns HTTP 200 with JSON array")
    void endpoints_returnsOkWithJsonArray() throws Exception {
        mockMvc.perform(get("/agentic-docs/api/endpoints")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /agentic-docs/api/chat — returns HTTP 405 (endpoint is POST-only)")
    void chatGet_returns405_methodNotAllowed() throws Exception {
        mockMvc.perform(get("/agentic-docs/api/chat")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("POST /agentic-docs/api/chat with blank question — returns HTTP 400 (Bean Validation)")
    void chatPost_withBlankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/agentic-docs/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /agentic-docs/api/chat with missing question field — returns HTTP 400")
    void chatPost_withMissingQuestion_returns400() throws Exception {
        mockMvc.perform(post("/agentic-docs/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /agentic-docs/api/chat with whitespace-only question — returns HTTP 400")
    void chatPost_withWhitespaceOnlyQuestion_returns400() throws Exception {
        mockMvc.perform(post("/agentic-docs/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }
}
