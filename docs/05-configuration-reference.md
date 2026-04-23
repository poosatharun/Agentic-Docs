# 05 — Configuration Reference

## All Supported Properties

### Agentic Docs Properties

| Property | Type | Default | Required | Description |
|---|---|---|---|---|
| `agentic.docs.enabled` | `boolean` | `false` | **Yes** | Master switch. Must be `true` to activate the starter. |

### OpenAI Properties (via Spring AI)

| Property | Type | Default | Description |
|---|---|---|---|
| `spring.ai.openai.api-key` | `String` | — | Your OpenAI API key. Use env var `SPRING_AI_OPENAI_API_KEY`. |
| `spring.ai.openai.chat.options.model` | `String` | `gpt-4o` | Chat model. Recommended: `gpt-4o-mini` for cost efficiency. |
| `spring.ai.openai.embedding.options.model` | `String` | `text-embedding-ada-002` | Embedding model. Recommended: `text-embedding-3-small`. |
| `spring.ai.openai.base-url` | `String` | `https://api.openai.com` | Override for Azure OpenAI or local proxies. |

### Recommended `application.properties`

```properties
server.port=8080

# ── Agentic Docs ──────────────────────────────────────────────────────────────
agentic.docs.enabled=true

# ── OpenAI ────────────────────────────────────────────────────────────────────
spring.ai.openai.api-key=${SPRING_AI_OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.embedding.options.model=text-embedding-3-small

# ── Logging ───────────────────────────────────────────────────────────────────
logging.level.com.agentic.docs=INFO
```

---

## Environment-Specific Configuration

### Enable only in development

```properties
# application.properties (production — Agentic Docs OFF)
agentic.docs.enabled=false

# application-dev.properties (development — Agentic Docs ON)
agentic.docs.enabled=true
spring.ai.openai.api-key=${SPRING_AI_OPENAI_API_KEY}
```

Activate with: `--spring.profiles.active=dev`

### Docker / Kubernetes

```yaml
# docker-compose.yml
environment:
  - SPRING_AI_OPENAI_API_KEY=sk-...
  - AGENTIC_DOCS_ENABLED=true
```

Spring Boot automatically maps `AGENTIC_DOCS_ENABLED` → `agentic.docs.enabled` via relaxed binding.

---

## Swapping the LLM Provider

The starter defaults to OpenAI, but Spring AI supports many providers. Swapping requires:
1. Removing the OpenAI starter from the host app's POM
2. Adding the desired provider's starter
3. Updating properties

### Azure OpenAI

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-azure-openai-spring-boot-starter</artifactId>
</dependency>
```

```properties
spring.ai.azure.openai.api-key=${AZURE_OPENAI_API_KEY}
spring.ai.azure.openai.endpoint=https://your-resource.openai.azure.com/
spring.ai.azure.openai.chat.options.deployment-name=gpt-4o-mini
spring.ai.azure.openai.embedding.options.deployment-name=text-embedding-3-small
```

### Ollama (Local, Free)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text
```

Run Ollama locally: `ollama pull llama3.2 && ollama pull nomic-embed-text`

### Anthropic Claude

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
```

```properties
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-3-5-haiku-20241022
```

Note: Anthropic does not provide an embedding model. You would need to keep the OpenAI embedding dependency alongside the Anthropic chat dependency.

---

## Swapping the Vector Store

To use a persistent vector store instead of the in-memory `SimpleVectorStore`:

### PostgreSQL + pgvector

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=postgres
spring.datasource.password=secret
spring.ai.vectorstore.pgvector.initialize-schema=true
```

Remove the `spring-ai-simple-vector-store` dependency from the starter POM and add the pgvector one. The `VectorStore` bean in `VectorStoreConfig` will be replaced by the pgvector auto-configuration.

### Pinecone

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pinecone-store-spring-boot-starter</artifactId>
</dependency>
```

```properties
spring.ai.vectorstore.pinecone.api-key=${PINECONE_API_KEY}
spring.ai.vectorstore.pinecone.index-name=agentic-docs
```
