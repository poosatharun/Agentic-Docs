# 03 — Backend Deep Dive

## Package Structure

```
com.agentic.docs.core
├── scanner/
│   ├── ApiEndpointMetadata.java     ← DTO record
│   └── ApiMetadataScanner.java      ← Endpoint discovery
├── config/
│   └── VectorStoreConfig.java       ← SimpleVectorStore bean (fallback)
├── ingestor/
│   └── ApiDocumentIngestor.java     ← Embedding + storage
└── chat/
    └── AgenticDocsChatController.java ← RAG chat endpoint

com.agentic.docs.autoconfigure
└── AgenticDocsAutoConfiguration.java ← Spring Boot AutoConfig (enabled by default)
```

---

## `ApiEndpointMetadata` — The Data Contract

**File:** `agentic-docs-core/src/main/java/com/agentic/docs/core/scanner/ApiEndpointMetadata.java`

```java
public record ApiEndpointMetadata(
        String path,
        String httpMethod,
        String controllerName,
        String methodName,
        String description,
        List<String> pathParams,      // @PathVariable names
        List<String> queryParams,     // @RequestParam names
        String requestBodyType,       // @RequestBody parameter simple class name
        String responseType           // return type, unwrapped from ResponseEntity<T>
) {
    public String toLlmReadableText() {
        return """
                Endpoint      : [%s] %s
                Controller    : %s
                Method        : %s
                Path Params   : %s
                Query Params  : %s
                Request Body  : %s
                Response Type : %s
                Summary       : %s
                """.formatted(
                httpMethod, path, controllerName, methodName,
                pathParams.isEmpty() ? "none" : String.join(", ", pathParams),
                queryParams.isEmpty() ? "none" : String.join(", ", queryParams),
                requestBodyType != null ? requestBodyType : "none",
                responseType != null ? responseType : "void",
                description);
    }
}
```

### Design Decisions

**Why a `record`?**  
Records are immutable by default in Java 16+. Endpoint metadata never changes after scanning — immutability prevents accidental mutation and makes the object safe to share across threads without synchronization.

**Why `toLlmReadableText()`?**  
The text that gets embedded into the vector store determines the quality of similarity search. A structured, labeled format (`Endpoint: [GET] /api/v1/subscriptions/{id}`) gives the embedding model clear semantic signals. Raw JSON or a flat string would produce lower-quality embeddings.

**Why include `controllerName` and `methodName`?**  
When the LLM generates code, it can reference the actual Java class and method name. This makes generated snippets more accurate and immediately usable in the host codebase.

**Why add `pathParams`, `queryParams`, `requestBodyType`, `responseType`?**  
These four fields close the gap between Swagger and Agentic Docs. Without them, a developer looking at an endpoint in the API Explorer could see the path and HTTP method but not the inputs or outputs — exactly the same limitation as Swagger's collapsed view. With them, a single click reveals the full contract. The fields are also injected into the LLM context via `toLlmReadableText()`, so the AI can answer questions like *"what fields does the request body require?"* accurately.

---

## `ApiMetadataScanner` — Endpoint Discovery

**File:** `agentic-docs-core/src/main/java/com/agentic/docs/core/scanner/ApiMetadataScanner.java`

### How it works

The scanner implements `ApplicationListener<ContextRefreshedEvent>`. Spring fires this event after the application context is fully initialized — meaning all beans are created and all `@RequestMapping` annotations have been processed.

```java
@Override
public void onApplicationEvent(ContextRefreshedEvent event) {
    if (!scannedEndpoints.isEmpty()) return; // guard against duplicate events

    Map<RequestMappingInfo, HandlerMethod> handlerMethods =
            handlerMapping.getHandlerMethods();

    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
        // filter to @RestController only
        // extract path, method, controller, description
    }
}
```

### Why `RequestMappingHandlerMapping` instead of reflection?

Two alternatives were considered:

1. **Reflection over `@RestController` beans** — would require iterating all beans, checking annotations, and manually parsing `@GetMapping`, `@PostMapping`, etc. Fragile and verbose.
2. **`RequestMappingHandlerMapping.getHandlerMethods()`** — Spring has already done all this work. It returns a complete, authoritative map of every registered route. This is the correct Spring-idiomatic approach.

### The duplicate-event guard

Spring can fire `ContextRefreshedEvent` multiple times in applications with parent/child contexts (e.g., Spring MVC creates a child `WebApplicationContext`). The guard `if (!scannedEndpoints.isEmpty()) return` prevents double-scanning.

### Reflective `@Operation` reading

```java
private String extractDescription(Method method) {
    try {
        Class<?> operationAnnotation = Class.forName("io.swagger.v3.oas.annotations.Operation");
        Object annotation = method.getAnnotation(...);
        // read summary via reflection
    } catch (Exception ignored) {
        // springdoc not on classpath — graceful fallback
    }
    return "No description provided.";
}
```

The `swagger-annotations` dependency is marked `optional` in the core POM. If the host app does not use springdoc, the class simply won't be found and the scanner falls back to "No description provided." This keeps the core module lightweight and non-prescriptive.

### Inputs & Outputs Extraction

Four additional helper methods use Spring's `MethodParameter` API to extract contract information from each handler method:

```java
// Reads @PathVariable name (falls back to Java parameter name)
private List<String> extractPathParams(HandlerMethod hm) { ... }

// Reads @RequestParam name (falls back to Java parameter name)
private List<String> extractQueryParams(HandlerMethod hm) { ... }

// Finds the first @RequestBody parameter and returns its simple class name
private String extractRequestBodyType(HandlerMethod hm) { ... }

// Reads the generic return type, unwrapping ResponseEntity<T> to T
private String extractResponseType(HandlerMethod hm) { ... }
```

`MethodParameter` is Spring's abstraction over `java.lang.reflect.Parameter`. It preserves parameter names from the compiled bytecode (requires `-parameters` compiler flag, which Spring Boot enables by default) and gives direct access to each annotation.

For `ResponseEntity<UserResponse>`, the method inspects the `ParameterizedType` generic argument and strips the `ResponseEntity` wrapper — the UI then shows `UserResponse` rather than the confusing raw type.

---

## `VectorStoreConfig` — In-Memory Vector Store

**File:** `agentic-docs-core/src/main/java/com/agentic/docs/core/config/VectorStoreConfig.java`

```java
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
```

### Why `SimpleVectorStore`?

`SimpleVectorStore` is Spring AI's in-memory implementation. It stores embeddings as `float[]` arrays in a `HashMap` and performs cosine similarity search in O(n) time.

**Trade-offs:**

| Aspect | SimpleVectorStore | Production alternative (e.g., pgvector) |
|---|---|---|
| Setup | Zero — no config needed | Requires DB, schema, connection pool |
| Persistence | Lost on restart | Persisted to disk |
| Scale | Suitable for ~10,000 docs | Millions of docs |
| Latency | Sub-millisecond (in-process) | Network round-trip |

For a starter library that needs to work out of the box with zero infrastructure, `SimpleVectorStore` is the correct choice. A production team can swap it for pgvector or Pinecone by simply adding a different Spring AI vector store dependency — the `VectorStore` interface is the same.

---

## `ApiDocumentIngestor` — Embedding and Storage

**File:** `agentic-docs-core/src/main/java/com/agentic/docs/core/ingestor/ApiDocumentIngestor.java`

```java
@EventListener(ContextRefreshedEvent.class)
@Order(1)
public void ingest() {
    if (!ingested.compareAndSet(false, true)) return;

    List<Document> documents = scanner.getScannedEndpoints().stream()
            .map(e -> new Document(
                    e.toLlmReadableText(),
                    Map.of(
                        "path",       e.path(),
                        "httpMethod", e.httpMethod(),
                        "controller", e.controllerName(),
                        "method",     e.methodName()
                    )
            ))
            .toList();

    vectorStore.add(documents);
}
```

### Why `@Order(1)` on the event listener?

Both `ApiMetadataScanner` (via `ApplicationListener`) and `ApiDocumentIngestor` (via `@EventListener`) respond to `ContextRefreshedEvent`. The ingestor must run after the scanner has populated `scannedEndpoints`. `@Order(1)` ensures the ingestor's listener fires after the scanner's (which has default order 0).

### Why `AtomicBoolean` instead of `synchronized`?

`compareAndSet(false, true)` is a lock-free atomic operation. It is simpler and more performant than a `synchronized` block for a one-time initialization guard. If two threads somehow race on the event (unlikely but possible in test contexts), only one will proceed.

### Document metadata

Each `Document` carries a metadata `Map` alongside the text. This metadata is stored in the vector store and returned with search results. It allows the chat controller to access structured fields (path, httpMethod) from retrieved documents if needed for future enhancements like generating curl commands.

---

## `AgenticDocsChatController` — The RAG Endpoint

**File:** `agentic-docs-core/src/main/java/com/agentic/docs/core/chat/AgenticDocsChatController.java`

### The System Prompt

```
You are an expert API assistant embedded inside developer documentation.
Your sole job is to help developers understand and use the REST APIs of THIS application.

Rules:
- Answer ONLY using the API context provided below. Do not invent endpoints.
- When asked for implementation, generate concise, correct Java or React code snippets
  using the exact paths, HTTP methods, and field names from the context.
- If the answer cannot be derived from the context, say:
  "I could not find a relevant endpoint for that. Please check the Swagger UI."
- Keep answers focused and developer-friendly.

API Context:
---
{context}
---
```

### Why a strict system prompt?

Without constraints, LLMs hallucinate. They will invent plausible-sounding endpoints that do not exist. The system prompt has three critical constraints:

1. **"Answer ONLY using the API context"** — grounds the model in retrieved facts
2. **"Do not invent endpoints"** — explicit prohibition against hallucination
3. **Fallback instruction** — tells the model what to say when it doesn't know, preventing confident wrong answers

### The RAG call

```java
List<Document> relevant = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(request.question())
                .topK(5)
                .build()
);

String context = relevant.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n---\n"));

String answer = chatClient.prompt()
        .system(s -> s.text(SYSTEM_PROMPT).param("context", context))
        .user(request.question())
        .call()
        .content();
```

**topK=5** — retrieves the 5 most semantically similar endpoints. This is enough context for most questions without exceeding token limits. For a 150-endpoint API, 5 documents is roughly 500 tokens of context.

**`@CrossOrigin(origins = "*")`** — allows the Vite dev server (port 5173) to call the Spring Boot backend (port 8080) during development without CORS errors.

---

## `AgenticDocsAutoConfiguration` — The Starter Wiring

**File:** `agentic-docs-spring-boot-starter/src/main/java/com/agentic/docs/autoconfigure/AgenticDocsAutoConfiguration.java`

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "agentic.docs", name = "enabled", havingValue = "true")
@ComponentScan(basePackages = "com.agentic.docs.core")
public class AgenticDocsAutoConfiguration {
}
```

### Why `@ConditionalOnProperty`?

This is the opt-in gate. Without it, adding the starter dependency would activate the agent in every environment including production. The property `agentic.docs.enabled=true` must be explicitly set. Teams can enable it only in development or staging environments.

### Why `@ComponentScan` instead of explicit `@Bean` methods?

The core module uses `@Component`, `@Configuration`, and `@RestController` annotations. `@ComponentScan` discovers them all automatically. Explicit `@Bean` methods would require the autoconfiguration to know about every class in core — a tight coupling that would break every time a new component is added.

### `@ConditionalOnWebApplication(type = SERVLET)`

Prevents activation in reactive (WebFlux) applications where `RequestMappingHandlerMapping` is not available. This is a defensive guard.

### `AutoConfiguration.imports`

```
com.agentic.docs.autoconfigure.AgenticDocsAutoConfiguration
```

This file in `META-INF/spring/` is the Spring Boot 3.x mechanism for registering auto-configurations. It replaces the old `spring.factories` file. Spring Boot reads this file at startup and conditionally activates the listed classes.
