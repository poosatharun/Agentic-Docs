package com.agentic.docs.core.chat;

import com.agentic.docs.core.config.AgenticDocsProperties;
import com.agentic.docs.core.model.ChatRequest;
import com.agentic.docs.core.model.ChatResponse;
import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.ApiMetadataScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-layer tests for {@link AgenticDocsChatController}.
 *
 * <h2>What is @WebMvcTest?</h2>
 * <p>{@code @WebMvcTest} starts only the web layer of Spring (controllers, filters,
 * MVC configuration) — NOT the full application context. This makes tests very fast.
 * Any beans the controller needs (like the service) must be provided as {@code @MockBean}.</p>
 *
 * <h2>What is MockMvc?</h2>
 * <p>{@code MockMvc} lets us simulate HTTP calls (GET, POST, etc.) and verify:
 * <ul>
 *   <li>The HTTP status code (200, 400, 405, ...)</li>
 *   <li>The response body JSON</li>
 *   <li>Response headers</li>
 * </ul>
 * All without starting a real HTTP server.</p>
 */
@WebMvcTest(AgenticDocsChatController.class)
@Import(AgenticDocsChatControllerTest.TestConfig.class)
class AgenticDocsChatControllerTest {

    /**
     * Provides the {@link AgenticDocsProperties} bean required by
     * {@code AgenticDocsMvcConfigurer} in the web-layer context.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        AgenticDocsProperties agenticDocsProperties() {
            return new AgenticDocsProperties(
                    true, 5, null,
                    new AgenticDocsProperties.Cors(List.of("http://localhost:5173"))
            );
        }
    }

    /** Spring injects MockMvc automatically when using @WebMvcTest. */
    @Autowired
    private MockMvc mockMvc;

    /** Used to serialize Java objects into JSON strings for request bodies. */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * @MockBean creates a Mockito mock AND registers it as a Spring bean.
     * The controller will get this fake service injected — no real LLM calls.
     * Injected as the {@link ChatService} interface, which is what the controller declares.
     */
    @MockBean
    private ChatService chatService;

    @MockBean
    private ApiMetadataScanner apiMetadataScanner;

    // ── POST /agentic-docs/api/chat ───────────────────────────────────────────

    @Test
    @DisplayName("POST /chat with valid question returns 200 and LLM answer")
    void postChat_withValidQuestion_returns200() throws Exception {
        // GIVEN: the service will return this answer
        when(chatService.answer(any())).thenReturn(new ChatResponse("Use POST /api/users"));

        // WHEN: we POST a valid JSON request body
        mockMvc.perform(post("/agentic-docs/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("How do I create a user?"))))
                // THEN: we get HTTP 200 with the answer in the response body
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Use POST /api/users"));
    }

    @Test
    @DisplayName("POST /chat with blank question returns 400 Bad Request")
    void postChat_withBlankQuestion_returns400() throws Exception {
        // WHEN: we POST a blank question
        mockMvc.perform(post("/agentic-docs/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"\"}"))
                // THEN: we get HTTP 400 — the controller rejects it before calling the service
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chat with null question returns 400 Bad Request")
    void postChat_withNullQuestion_returns400() throws Exception {
        // WHEN: we POST a null question
        mockMvc.perform(post("/agentic-docs/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": null}"))
                // THEN: we get HTTP 400
                .andExpect(status().isBadRequest());
    }

    // ── GET /agentic-docs/api/chat (wrong method hint) ────────────────────────

    @Test
    @DisplayName("GET /chat returns 405 Method Not Allowed with helpful message")
    void getChat_returns405() throws Exception {
        mockMvc.perform(get("/agentic-docs/api/chat"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.answer").value(
                        org.hamcrest.Matchers.containsString("only accepts POST requests")));
    }

    // ── GET /agentic-docs/api/endpoints ──────────────────────────────────────

    @Test
    @DisplayName("GET /endpoints returns 200 with the list of scanned endpoints")
    void getEndpoints_returns200WithEndpointList() throws Exception {
        // GIVEN: the scanner has found one endpoint
        ApiEndpointMetadata fakeEndpoint =
                new ApiEndpointMetadata("/api/users", "GET", "UserController", "getAllUsers", "List all users", List.of(), List.of(), null, null);
        when(apiMetadataScanner.getScannedEndpoints()).thenReturn(List.of(fakeEndpoint));

        // WHEN: we call the endpoints list API
        mockMvc.perform(get("/agentic-docs/api/endpoints"))
                // THEN: we get HTTP 200 and the endpoint appears in the JSON array
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("/api/users"))
                .andExpect(jsonPath("$[0].httpMethod").value("GET"))
                .andExpect(jsonPath("$[0].controllerName").value("UserController"));
    }

    @Test
    @DisplayName("GET /endpoints returns 200 with empty list when no endpoints are scanned")
    void getEndpoints_returnsEmptyList_whenNoneScanned() throws Exception {
        when(apiMetadataScanner.getScannedEndpoints()).thenReturn(List.of());

        mockMvc.perform(get("/agentic-docs/api/endpoints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
