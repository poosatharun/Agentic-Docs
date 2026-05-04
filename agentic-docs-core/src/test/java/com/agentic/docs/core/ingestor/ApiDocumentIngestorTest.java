package com.agentic.docs.core.ingestor;

import com.agentic.docs.core.scanner.ApiEndpointMetadata;
import com.agentic.docs.core.scanner.EndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApiDocumentIngestor}.
 *
 * <p>These tests verify the core ingestion logic:
 * <ul>
 *   <li>Endpoints from the scanner are converted to {@link Document} objects</li>
 *   <li>Documents are added to the vector store exactly once</li>
 *   <li>Duplicate event fires are safely ignored (idempotency guard)</li>
 *   <li>Graceful handling when no endpoints are scanned</li>
 * </ul>
 *
 * <h2>Key concept: ArgumentCaptor</h2>
 * <p>An {@code ArgumentCaptor} lets us "capture" the actual argument passed to a mock
 * method, so we can inspect its contents in assertions. Here we capture the list of
 * {@link Document} objects passed to {@code vectorStore.add(...)} to verify they
 * were built correctly from the endpoint metadata.</p>
 */
@ExtendWith(MockitoExtension.class)
class ApiDocumentIngestorTest {

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private VectorStore vectorStore;

    private ApiDocumentIngestor ingestor;

    @BeforeEach
    void setUp() {
        ingestor = new ApiDocumentIngestor(endpointRepository, vectorStore);
    }

    @Test
    @DisplayName("ingest() adds one Document per scanned endpoint")
    void ingest_addsOneDocumentPerEndpoint() {
        // GIVEN: the scanner found two endpoints
        List<ApiEndpointMetadata> endpoints = List.of(
                new ApiEndpointMetadata("/api/users",    "GET",  "UserController",    "getUsers",    "List users",    List.of(), List.of(), null, null),
                new ApiEndpointMetadata("/api/payments", "POST", "PaymentController", "makePayment", "Make a payment", List.of(), List.of(), "PaymentRequest", "PaymentResponse")
        );
        when(endpointRepository.getScannedEndpoints()).thenReturn(endpoints);

        // WHEN: ingestion runs
        ingestor.ingest();

        // THEN: vectorStore.add() was called with exactly 2 documents
        // We use ArgumentCaptor to capture what was actually passed to vectorStore.add()
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> addedDocs = captor.getValue();
        assertThat(addedDocs).hasSize(2);
    }

    @Test
    @DisplayName("ingest() embeds endpoint details in the document text")
    void ingest_embeds_endpointDetails_inDocumentText() {
        // GIVEN: one endpoint
        List<ApiEndpointMetadata> endpoints = List.of(
                new ApiEndpointMetadata("/api/orders", "DELETE", "OrderController", "cancelOrder", "Cancel an order", List.of(), List.of(), null, "void")
        );
        when(endpointRepository.getScannedEndpoints()).thenReturn(endpoints);

        // WHEN
        ingestor.ingest();

        // THEN: capture the document and verify its text contains key info
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        String docText = captor.getValue().get(0).getText();
        assertThat(docText).contains("/api/orders");
        assertThat(docText).contains("DELETE");
        assertThat(docText).contains("Cancel an order");
    }

    @Test
    @DisplayName("ingest() does not call vectorStore when no endpoints are found")
    void ingest_skipsVectorStore_whenNoEndpoints() {
        // GIVEN: the repository found nothing
        when(endpointRepository.getScannedEndpoints()).thenReturn(List.of());

        // WHEN
        ingestor.ingest();

        // THEN: vectorStore.add() was NEVER called — no point storing empty data
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("ingest() is idempotent — second call is silently ignored")
    void ingest_isIdempotent_secondCallIgnored() {
        // GIVEN: one endpoint available
        when(endpointRepository.getScannedEndpoints()).thenReturn(List.of(
                new ApiEndpointMetadata("/api/users", "GET", "UserController", "getUsers", "List users", List.of(), List.of(), null, null)
        ));

        // WHEN: ingest() is called twice (simulating Spring context refreshed twice)
        ingestor.ingest();
        ingestor.ingest();

        // THEN: vectorStore.add() was called only once — the AtomicBoolean guard worked
        verify(vectorStore, times(1)).add(any());
    }

    @Test
    @DisplayName("ingest() continues gracefully when vectorStore.add() throws an exception")
    void ingest_continuesGracefully_whenVectorStoreThrows() {
        // GIVEN: one endpoint available
        when(endpointRepository.getScannedEndpoints()).thenReturn(List.of(
                new ApiEndpointMetadata("/api/users", "GET", "UserController", "getUsers", "List users", List.of(), List.of(), null, null)
        ));
        // AND: the vector store throws an error (e.g. embedding model unavailable)
        doThrow(new RuntimeException("Embedding model not available")).when(vectorStore).add(any());

        // WHEN / THEN: ingest() should NOT propagate the exception — just log a warning
        // If an exception were thrown here, the test would fail with an error.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> ingestor.ingest());
    }
}
