package com.agentic.docs.flow.controller;

import com.agentic.docs.flow.executor.FlowExecutorService;
import com.agentic.docs.flow.model.FlowRequest;
import com.agentic.docs.flow.registry.FlowSseRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes the Flow Tracer API used by the React frontend.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * POST /agentic-docs/api/flow/execute
 *   Body:     FlowRequest  { httpMethod, path, pathParams, body }
 *   Response: { "traceId": "uuid" }   — returned immediately (async execution)
 *
 * GET /agentic-docs/api/flow/trace/{traceId}
 *   Produces: text/event-stream
 *   Events:
 *     event: step   data: { TraceEvent JSON }    — one per intercepted method call
 *     event: done   data: { FlowDoneEvent JSON } — final HTTP result
 *     event: error  data: { "message": "..." }   — network / fatal error
 * </pre>
 */
@RestController
@RequestMapping("/agentic-docs/api/flow")
public class FlowController {

    private final FlowSseRegistry    registry;
    private final FlowExecutorService executor;

    public FlowController(FlowSseRegistry registry, FlowExecutorService executor) {
        this.registry = registry;
        this.executor = executor;
    }

    /**
     * Starts an async traced execution.
     * Returns the {@code traceId} immediately so the frontend can open the SSE stream
     * before (or concurrently with) the execution starting.
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> execute(@RequestBody FlowRequest request) {
        String traceId = UUID.randomUUID().toString();
        registry.register(traceId);
        executor.executeAsync(traceId, request);
        return ResponseEntity.accepted().body(Map.of("traceId", traceId));
    }

    /**
     * SSE endpoint — browser connects here after receiving the {@code traceId}.
     * Replays any events buffered before the connection arrived, then streams live.
     */
    @GetMapping(value = "/trace/{traceId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter trace(@PathVariable String traceId) {
        return registry.attach(traceId);
    }
}
