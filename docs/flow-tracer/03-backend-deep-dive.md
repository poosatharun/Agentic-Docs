# 03 — Flow Tracer: Backend Deep Dive

## Package Overview

```
com.agentic.docs.flow
├── model
│   ├── TraceEvent.java
│   ├── FlowRequest.java
│   └── FlowDoneEvent.java
├── registry
│   └── FlowSseRegistry.java
├── aspect
│   └── FlowAspect.java
├── executor
│   └── FlowExecutorService.java
├── controller
│   └── FlowController.java
└── autoconfigure
    └── AgenticDocsFlowAutoConfiguration.java
```

---

## model/TraceEvent.java

Represents a single method-call snapshot during a trace.

```java
public record TraceEvent(
    String traceId,
    int    stepIndex,   // 0-based, assigned at method ENTRY (not exit)
    String layer,       // "CONTROLLER" | "SERVICE" | "REPOSITORY"
    String className,
    String methodName,
    String inputJson,   // JSON-serialised args, capped at 2 KB
    String outputJson,  // JSON-serialised return value, capped at 2 KB
    long   durationMs,
    String status,      // "EXIT" | "ERROR"
    String errorMessage // null unless status == "ERROR"
) {}
```

**Design notes:**
- `stepIndex` is assigned at method *entry* (before `proceed()`), not at exit.
  This ensures the index reflects the call order, not the completion order.
  Nested calls (e.g. `ServiceA` calls `ServiceB`) get lower indices for the
  outer call but that outer call completes *after* the inner call — so SSE events
  arrive in completion order. The UI sorts by `stepIndex` to restore call order.
- `inputJson` and `outputJson` are capped at 2 048 characters. If truncated, the
  string ends with `"[TRUNCATED]"`.

---

## model/FlowRequest.java

The body the UI sends to `POST /agentic-docs/api/flow/execute`.

```java
public record FlowRequest(
    String              httpMethod,   // "GET" | "POST" | "PUT" | "DELETE" | "PATCH"
    String              path,         // "/api/v1/checkout"
    Map<String, String> pathParams,   // { "id": "123" } → replaces {id} in path
    String              body          // raw JSON string for POST/PUT/PATCH
) {}
```

---

## model/FlowDoneEvent.java

The final SSE event sent after the HTTP call completes.

```java
public record FlowDoneEvent(
    String traceId,
    int    httpStatus,    // e.g. 200
    String responseBody,  // raw JSON from the endpoint
    long   totalMs,       // wall-clock time from execute() call
    boolean ok,           // httpStatus < 400
    int    stepCount      // number of TraceEvents captured
) {}
```

---

## registry/FlowSseRegistry.java

The central coordinator for all live traces. It holds:
- A buffer of events pushed before the SSE client connects
- The `SseEmitter` once the client connects
- A counter for assigning `stepIndex` values

```java
@Component
public class FlowSseRegistry {

    // Per-trace entry
    private static class TraceEntry {
        final CopyOnWriteArrayList<BufferedEvent> buffer = new CopyOnWriteArrayList<>();
        volatile SseEmitter emitter;
        final AtomicInteger stepCounter = new AtomicInteger(0);
    }

    private final ConcurrentHashMap<String, TraceEntry> entries = new ConcurrentHashMap<>();

    public void register(String traceId) { ... }

    // Called when the browser opens GET /trace/{traceId}
    public SseEmitter attach(String traceId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        entry.emitter = emitter;
        // replay buffered events immediately so late-connecting clients
        // don't miss steps that fired before they connected
        for (BufferedEvent e : entry.buffer) {
            emitter.send(SseEmitter.event().name(e.eventName()).data(e.data()));
        }
        return emitter;
    }

    public int nextStepIndex(String traceId) {
        return entry.stepCounter.getAndIncrement();
    }

    public void pushStep(String traceId, TraceEvent event) { ... }
    public void pushDone(String traceId, FlowDoneEvent event) { ... }
    public void pushError(String traceId, String message) { ... }
}
```

**Thread safety:**
- `CopyOnWriteArrayList` allows the AOP thread (writer) and the SSE completion
  thread (reader, during replay) to coexist safely.
- `ConcurrentHashMap` protects the outer map.
- `volatile SseEmitter emitter` ensures visibility across threads when the
  SSE client attaches after execution starts.

**Late-connect replay:**
The browser opens the SSE connection *after* receiving the `traceId`, but the
virtual-thread execution may have already fired several AOP events. The `attach()`
method immediately replays the buffer so no steps are lost.

---

## aspect/FlowAspect.java

The core interception logic. Uses Spring AOP with a proxy-based `@Around` advice.

```java
@Aspect
@Component
public class FlowAspect {

    @Around("""
        (within(@org.springframework.stereotype.Service *)
         || within(@org.springframework.web.bind.annotation.RestController *)
         || within(@org.springframework.stereotype.Repository *))
        && !within(com.agentic.docs.flow..*)
        && !within(com.agentic.docs.core..*)
        """)
    public Object traceMethod(ProceedingJoinPoint pjp) throws Throwable {

        // 1. Check if this request carries a trace ID
        String traceId = extractTraceId();
        if (traceId == null) return pjp.proceed(); // pass-through, zero overhead

        // 2. Reserve a step index BEFORE calling proceed()
        //    so that nested calls get higher indices
        int stepIndex = registry.nextStepIndex(traceId);
        String layer  = resolveLayer(pjp);
        String input  = serialize(pjp.getArgs());

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long  dur     = System.currentTimeMillis() - start;

            registry.pushStep(traceId, new TraceEvent(
                traceId, stepIndex, layer,
                pjp.getTarget().getClass().getSimpleName(),
                pjp.getSignature().getName(),
                input, serialize(result), dur, "EXIT", null
            ));
            return result;

        } catch (Throwable ex) {
            long dur = System.currentTimeMillis() - start;
            registry.pushStep(traceId, new TraceEvent(
                traceId, stepIndex, layer,
                pjp.getTarget().getClass().getSimpleName(),
                pjp.getSignature().getName(),
                input, null, dur, "ERROR", ex.getMessage()
            ));
            throw ex;
        }
    }

    private String extractTraceId() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return attrs.getRequest().getHeader("X-Flow-Trace-Id");
        } catch (Exception e) { return null; }
    }

    private String resolveLayer(ProceedingJoinPoint pjp) {
        Class<?> target = pjp.getTarget().getClass();
        if (target.isAnnotationPresent(Repository.class))     return "REPOSITORY";
        if (target.isAnnotationPresent(Service.class))        return "SERVICE";
        if (target.isAnnotationPresent(RestController.class)) return "CONTROLLER";
        return "UNKNOWN";
    }
}
```

**Critical pointcut exclusions:**

| Excluded package | Reason |
|---|---|
| `com.agentic.docs.flow..*` | Avoid intercepting `FlowController` and `FlowExecutorService` itself — would cause infinite recursion |
| `com.agentic.docs.core..*` | Avoid intercepting internal framework beans (metrics, vector store, chat) |

> **Do NOT exclude `com.agentic.docs.sample..*`** — that is the application
> code you want to trace. A common mistake is using `!within(com.agentic.docs..*)`,
> which incorrectly excludes the sample app too. See `09-bugs-and-fixes.md`.

---

## executor/FlowExecutorService.java

Responsible for firing the actual HTTP request to the target endpoint, on a
virtual thread, with the trace header attached.

```java
@Service
public class FlowExecutorService {

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    @Value("${server.port:8080}")
    private int serverPort;

    private final RestClient restClient = RestClient.create();
    private final FlowSseRegistry registry;
    private final ObjectMapper objectMapper;

    public void runAsync(FlowRequest request, String traceId) {
        Thread.ofVirtual().name("flow-trace-" + traceId).start(() -> {
            long start = System.currentTimeMillis();
            try {
                String path = resolvePathParams(request.path(), request.pathParams());
                String url  = "http://localhost:" + serverPort + path;

                RestClient.RequestBodySpec spec = restClient
                    .method(HttpMethod.valueOf(request.httpMethod()))
                    .uri(url)
                    .header("X-Flow-Trace-Id", traceId)
                    .header("Content-Type", "application/json");

                if (BODY_METHODS.contains(request.httpMethod()) && request.body() != null) {
                    spec.body(request.body());
                }

                ResponseEntity<String> resp = spec
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> {}) // never throw on 4xx/5xx
                    .toEntity(String.class);

                long totalMs = System.currentTimeMillis() - start;
                registry.pushDone(traceId, new FlowDoneEvent(
                    traceId,
                    resp.getStatusCode().value(),
                    resp.getBody(),
                    totalMs,
                    resp.getStatusCode().value() < 400,
                    registry.getStepCount(traceId)
                ));
            } catch (Exception ex) {
                registry.pushError(traceId, ex.getMessage());
            }
        });
    }
}
```

**Design notes:**
- `.onStatus(status -> true, ...)` prevents `RestClient` from throwing an
  exception on 4xx/5xx responses. This is important: even a `500` response
  from the target endpoint has a valid response body to display.
- `Thread.ofVirtual()` means there is no thread-pool limit. Thousands of
  concurrent traces are handled without blocking any Tomcat worker thread.
- `serverPort` is injected from `${server.port:8080}` — it defaults to 8080
  but respects whatever port the application is actually running on.

---

## controller/FlowController.java

Exposes two endpoints:

```java
@RestController
@RequestMapping("/agentic-docs/api/flow")
public class FlowController {

    @PostMapping("/execute")
    public Map<String, String> execute(@RequestBody FlowRequest request) {
        String traceId = UUID.randomUUID().toString();
        registry.register(traceId);
        executor.runAsync(request, traceId);
        return Map.of("traceId", traceId);
    }

    @GetMapping("/trace/{traceId}")
    public SseEmitter trace(@PathVariable String traceId) {
        return registry.attach(traceId);
    }
}
```

**Why two separate calls?**
The browser must open the SSE connection (`GET /trace/{id}`) *before* any
events can be received. But it also needs the `traceId` to open that connection.
The split works because:
1. `POST /execute` returns `traceId` immediately (the virtual thread hasn't
   even sent the first HTTP byte yet)
2. The browser opens `GET /trace/{traceId}` within milliseconds
3. Even if a few events fired before the SSE connection opens, the buffer
   in `FlowSseRegistry` replays them on attach

---

## autoconfigure/AgenticDocsFlowAutoConfiguration.java

```java
@AutoConfiguration
@ConditionalOnProperty(
    name = "agentic.docs.flow.enabled",
    havingValue = "true",
    matchIfMissing = false    // ← OFF by default
)
@EnableAspectJAutoProxy
@ComponentScan("com.agentic.docs.flow")
public class AgenticDocsFlowAutoConfiguration {
    // All beans are discovered via @ComponentScan
}
```

Registered in:
```
src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

```
com.agentic.docs.core.autoconfigure.AgenticDocsAutoConfiguration
com.agentic.docs.flow.autoconfigure.AgenticDocsFlowAutoConfiguration
```

Both auto-configurations are registered in the same file because the
`agentic-docs-spring-boot-starter` JAR (which hosts this file) bundles both
`agentic-docs-core` and `agentic-docs-flow`.
