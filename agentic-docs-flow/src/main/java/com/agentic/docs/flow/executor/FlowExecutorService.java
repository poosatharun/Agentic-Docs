package com.agentic.docs.flow.executor;

import com.agentic.docs.flow.aspect.FlowAspect;
import com.agentic.docs.flow.model.FlowDoneEvent;
import com.agentic.docs.flow.model.FlowRequest;
import com.agentic.docs.flow.spi.TraceEventSink;
import com.agentic.docs.flow.url.FlowUrlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Set;

/**
 * Executes a traced API request on a virtual thread so the main request thread
 * returns the {@code traceId} to the browser immediately.
 *
 * <p>Flow:
 * <ol>
 *   <li>Resolves path parameters in the URL template.</li>
 *   <li>Fires the HTTP call via {@link RestClient} with the {@code X-Flow-Trace-Id} header
 *       so that {@code FlowAspect} can correlate intercepted method calls to this trace.</li>
 *   <li>On completion pushes a {@link FlowDoneEvent} via {@link com.agentic.docs.flow.spi.TraceEventSink}.</li>
 *   <li>On network failure pushes an error event instead.</li>
 * </ol>
 *
 * <p>Error responses (4xx/5xx) are NOT thrown as exceptions — we always want the
 * full response body and status code in the final event.
 */
@Service
public class FlowExecutorService {

    private static final Logger log = LoggerFactory.getLogger(FlowExecutorService.class);

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    private final TraceEventSink sink;
    private final FlowUrlBuilder urlBuilder;
    private final RestClient     restClient;

    public FlowExecutorService(TraceEventSink sink, FlowUrlBuilder urlBuilder, RestClient restClient) {
        this.sink       = sink;
        this.urlBuilder = urlBuilder;
        this.restClient = restClient;
    }

    /**
     * Starts execution on a virtual thread. Returns immediately.
     *
     * @param traceId UUID that ties SSE events together; already registered via {@link com.agentic.docs.flow.spi.TraceEmitterProvider#register}.
     * @param request The API request to execute.
     */
    public void executeAsync(String traceId, FlowRequest request) {
        Thread.ofVirtual()
              .name("flow-tracer-" + traceId)
              .start(() -> execute(traceId, request));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void execute(String traceId, FlowRequest request) {
        long start = System.currentTimeMillis();

        try {
            String url = urlBuilder.build(request);
            log.debug("Flow [{}] → {} {}", traceId, request.httpMethod(), url);

            RestClient.RequestBodySpec spec = restClient
                    .method(HttpMethod.valueOf(request.httpMethod().toUpperCase()))
                    .uri(url)
                    .header(FlowAspect.TRACE_HEADER, traceId)
                    .header("Content-Type", "application/json");

            String rawBody = (request.body() != null && !request.body().isBlank())
                    ? request.body() : "{}";

            ResponseEntity<String> response;
            if (BODY_METHODS.contains(request.httpMethod().toUpperCase())) {
                response = spec.body(rawBody)
                        .retrieve()
                        .onStatus(status -> true, (req, res) -> {})
                        .toEntity(String.class);
            } else {
                response = spec.retrieve()
                        .onStatus(status -> true, (req, res) -> {})
                        .toEntity(String.class);
            }

            long totalMs = System.currentTimeMillis() - start;

            sink.pushDone(traceId, new FlowDoneEvent(
                    traceId,
                    response.getStatusCode().value(),
                    response.getBody() != null ? response.getBody() : "",
                    totalMs,
                    response.getStatusCode().is2xxSuccessful(),
                    0
            ));

        } catch (Exception ex) {
            log.warn("Flow [{}] executor error: {}", traceId, ex.getMessage());
            sink.pushError(traceId, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}
