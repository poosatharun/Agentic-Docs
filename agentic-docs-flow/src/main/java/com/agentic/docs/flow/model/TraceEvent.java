package com.agentic.docs.flow.model;

/**
 * A single method-call snapshot captured by {@link com.agentic.docs.flow.aspect.FlowAspect}.
 *
 * <p>One {@code TraceEvent} is pushed via SSE for every intercepted Spring bean method
 * that executes during a traced HTTP request.
 *
 * @param traceId      Unique ID for this execution trace (ties events together).
 * @param stepIndex    Zero-based position of this step in the execution chain.
 * @param layer        Bean stereotype: CONTROLLER | SERVICE | REPOSITORY | COMPONENT.
 * @param className    Simple class name of the bean (e.g. {@code OrderService}).
 * @param methodName   Method name (e.g. {@code createOrder}).
 * @param inputJson    JSON-serialized method arguments (capped at 2 KB; fallback toString).
 * @param outputJson   JSON-serialized return value (capped at 2 KB; fallback toString).
 * @param durationMs   Wall-clock time for the method call in milliseconds.
 * @param status       {@code EXIT} on success, {@code ERROR} on exception.
 * @param errorMessage Exception message when {@code status=ERROR}; null otherwise.
 */
public record TraceEvent(
        String traceId,
        int    stepIndex,
        String layer,
        String className,
        String methodName,
        String inputJson,
        String outputJson,
        long   durationMs,
        String status,
        String errorMessage
) {}
