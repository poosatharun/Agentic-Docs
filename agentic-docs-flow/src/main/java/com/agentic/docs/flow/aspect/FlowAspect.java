package com.agentic.docs.flow.aspect;

import com.agentic.docs.flow.model.TraceEvent;
import com.agentic.docs.flow.registry.FlowSseRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Spring AOP aspect that intercepts public method calls on {@code @Service},
 * {@code @RestController}, and {@code @Repository} beans belonging to the host application
 * during a traced HTTP request.
 *
 * <p>A request is considered "traced" only if the incoming HTTP request carries the header
 * {@code X-Flow-Trace-Id}. This means all normal (non-traced) requests pass through with
 * zero overhead beyond a header check.
 *
 * <p>The aspect skips any class inside {@code com.agentic.docs} to avoid recursively
 * tracing its own internals.
 *
 * <p>Input args and return values are serialized to JSON (capped at 2 KB).
 * Non-serializable types fall back to {@link Object#toString()}.
 */
@Aspect
@Component
public class FlowAspect {

    private static final Logger log = LoggerFactory.getLogger(FlowAspect.class);

    /** Header name injected by FlowExecutorService on outbound RestClient calls. */
    public static final String TRACE_HEADER = "X-Flow-Trace-Id";

    /** Maximum JSON size for input/output fields (2 KB). */
    private static final int MAX_JSON_BYTES = 2048;

    private final FlowSseRegistry registry;
    private final ObjectMapper    objectMapper = new ObjectMapper();

    /** Per-trace step counters so each step gets a unique ascending index. */
    private final Map<String, AtomicInteger> stepCounters = new ConcurrentHashMap<>();

    public FlowAspect(FlowSseRegistry registry) {
        this.registry = registry;
    }

    /**
     * Intercepts all public methods on @Service, @RestController, and @Repository beans,
     * excluding the flow/core/autoconfigure internals to prevent recursive self-tracing.
     */
    @Around("""
            (within(@org.springframework.stereotype.Service *)
             || within(@org.springframework.web.bind.annotation.RestController *)
             || within(@org.springframework.stereotype.Repository *))
            && !within(com.agentic.docs.flow..*)
            && !within(com.agentic.docs.core..*)
            && !within(com.agentic.docs.autoconfigure..*)
            """)
    public Object traceMethodCall(ProceedingJoinPoint pjp) throws Throwable {
        // Only trace if there is an active traced HTTP request on this thread
        String traceId = resolveTraceId();
        if (traceId == null) {
            return pjp.proceed();
        }

        MethodSignature sig       = (MethodSignature) pjp.getSignature();
        String          className = pjp.getTarget().getClass().getSimpleName();
        String          methodName= sig.getName();
        String          layer     = resolveLayer(pjp.getTarget());
        int             stepIndex = nextStep(traceId);

        String inputJson = serialize(pjp.getArgs());
        long   start     = System.currentTimeMillis();

        try {
            Object result    = pjp.proceed();
            long   durationMs = System.currentTimeMillis() - start;
            String outputJson = serialize(result);

            registry.pushStep(traceId, new TraceEvent(
                    traceId, stepIndex, layer, className, methodName,
                    inputJson, outputJson, durationMs, "EXIT", null));

            return result;

        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;

            registry.pushStep(traceId, new TraceEvent(
                    traceId, stepIndex, layer, className, methodName,
                    inputJson, null, durationMs, "ERROR", buildErrorMessage(ex)));

            throw ex;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Read the X-Flow-Trace-Id header from the current request thread. Returns null for normal requests. */
    private String resolveTraceId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader(TRACE_HEADER);
        } catch (Exception e) {
            return null;
        }
    }

    /** Determine the layer label from the bean's declared annotations. */
    private String resolveLayer(Object target) {
        Class<?> cls = target.getClass();
        if (cls.isAnnotationPresent(RestController.class)) return "CONTROLLER";
        if (cls.isAnnotationPresent(Service.class))        return "SERVICE";
        if (cls.isAnnotationPresent(Repository.class))     return "REPOSITORY";
        return "COMPONENT";
    }

    /** Get or create a step counter for the trace and return the next index. */
    private int nextStep(String traceId) {
        return stepCounters.computeIfAbsent(traceId, id -> new AtomicInteger(0))
                           .getAndIncrement();
    }

    /** Serialize one or more values to a capped JSON string. */
    private String serialize(Object value) {
        if (value == null) return "null";
        if (value instanceof Object[] arr) {
            // Method args array
            try {
                String json = objectMapper.writeValueAsString(arr);
                return cap(json);
            } catch (Exception e) {
                return cap(java.util.Arrays.toString(arr));
            }
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            return cap(json);
        } catch (Exception e) {
            return cap(value.toString());
        }
    }

    private String cap(String s) {
        if (s == null) return "null";
        return s.length() <= MAX_JSON_BYTES ? s : s.substring(0, MAX_JSON_BYTES) + "… [truncated]";
    }

    private String buildErrorMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder(ex.getClass().getName());
        if (ex.getMessage() != null) sb.append(": ").append(ex.getMessage());
        StackTraceElement[] trace = ex.getStackTrace();
        int lines = Math.min(trace.length, 10);
        for (int i = 0; i < lines; i++) {
            sb.append("\n  at ").append(trace[i]);
        }
        return sb.toString();
    }
}
