# Switching LLM Providers — Troubleshooting & Execution Guide

This guide documents the known errors encountered when switching between **OpenAI** and **Ollama** providers, explains why they happen, and provides exact step-by-step commands to switch correctly.

---

## How This Project's Provider System Works

This project uses **two separate profile systems** that must both be set to the same provider:

| System | Where | Controls |
|---|---|---|
| **Maven build profile** (`-P openai` / `-P ollama`) | `pom.xml` of `agentic-docs-spring-boot-starter` | Which AI provider JAR is on the **classpath** |
| **Spring runtime profile** (`spring.profiles.active`) | `application.properties` | Which `application-{provider}.properties` is loaded |

> ⚠️ **Changing only one of these will cause startup failures.** Both must match.

---

## Error 1 — SSL Certificate Failure (OpenAI behind Corporate Proxy)

### Symptom

```
org.springframework.web.client.ResourceAccessException: I/O error on POST request
for "https://api.openai.com/v1/embeddings": (certificate_unknown) PKIX path building
failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find
valid certification path to requested target
```

### Root Cause

- Your app starts → `ApiDocumentIngestor.ingest()` is triggered via `@EventListener`
- `SimpleVectorStore` calls `OpenAiEmbeddingModel` to detect embedding dimensions
- This POSTs to `https://api.openai.com/v1/embeddings`
- A **corporate SSL-inspection proxy** (e.g., Zscaler, Netskope) intercepts the HTTPS request and re-signs the certificate with its own CA
- Java's default `cacerts` truststore does **not** trust that corporate CA → **SSL handshake fails**

### Solutions

**Option A — Proper fix: Import corporate CA into Java truststore**

Ask your IT/network team for the corporate root CA certificate file, then:

```powershell
keytool -import -alias corp-proxy-ca `
  -keystore "$env:JAVA_HOME\lib\security\cacerts" `
  -file "C:\path\to\corp-ca.cer" `
  -storepass changeit `
  -noprompt
```

Restart the app after importing.

**Option B — Switch to Ollama (free, local, no SSL issues)**

See [Switching to Ollama](#switching-openai--ollama) below.

---

## Error 2 — OpenAI Bean Created When Ollama Profile Is Active

### Symptom

```
Error creating bean with name 'openAiEmbeddingModel': Failed to instantiate
[org.springframework.ai.openai.OpenAiEmbeddingModel]: Factory method
'openAiEmbeddingModel' threw exception with message: OpenAI API key must be set.
Use the connection property: spring.ai.openai.api-key
```

### Root Cause

Only `spring.profiles.active=ollama` was changed in `application.properties`, but the project was **not rebuilt** with the `ollama` Maven profile. Because the Maven `openai` profile is `activeByDefault=true`, the `spring-ai-starter-model-openai` JAR remained on the classpath. Spring Boot auto-configured `openAiEmbeddingModel`, which demanded an API key.

**Rule:** Changing `spring.profiles.active` alone is not enough — you must also **rebuild** with the matching Maven profile (`-P ollama` or `-P openai`).

---

## Switching: OpenAI → Ollama

### Prerequisites

1. Install Ollama: https://ollama.com/download
2. Pull required models (one-time setup):

```powershell
ollama pull llama3.2
ollama pull nomic-embed-text
```

3. Start Ollama (if not already running as a service):

```powershell
ollama serve
```

### Step 1 — Set the Spring runtime profile

Edit `agentic-docs-sample-app/src/main/resources/application.properties`:

```properties
spring.profiles.active=ollama
```

### Step 2 — Rebuild with the Ollama Maven profile

```powershell
cd "c:\Users\2432136\Downloads\new_folder\agentic_docs"
mvn clean install -P ollama -DskipTests
```

### Step 3 — Run the application

```powershell
cd agentic-docs-sample-app
mvn spring-boot:run
```

Or run the packaged JAR:

```powershell
java -jar target\agentic-docs-sample-app-1.0.0-SNAPSHOT.jar
```

### Verify Ollama is active

On startup you should see log lines like:

```
INFO  OllamaChatModel       - Using model: llama3.2
INFO  ApiDocumentIngestor   - Ingesting API documents...
```

---

## Switching: Ollama → OpenAI

### Prerequisites

1. Obtain an OpenAI API key: https://platform.openai.com/api-keys
2. Set it as an environment variable:

```powershell
$env:SPRING_AI_OPENAI_API_KEY = "sk-..."
```

To persist across sessions, add it to your system environment variables via:
> Control Panel → System → Advanced System Settings → Environment Variables

### Step 1 — Set the Spring runtime profile

Edit `agentic-docs-sample-app/src/main/resources/application.properties`:

```properties
spring.profiles.active=openai
```

### Step 2 — Rebuild with the OpenAI Maven profile

```powershell
cd "c:\Users\2432136\Downloads\new_folder\agentic_docs"
mvn clean install -P openai -DskipTests
```

### Step 3 — Run the application

```powershell
cd agentic-docs-sample-app
mvn spring-boot:run
```

Or run the packaged JAR:

```powershell
java -jar target\agentic-docs-sample-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=openai
```

### Verify OpenAI is active

On startup you should see log lines like:

```
INFO  OpenAiEmbeddingModel  - Using model: text-embedding-3-small
INFO  ApiDocumentIngestor   - Ingesting API documents...
```

---

## Quick Reference Cheat Sheet

| Action | Ollama | OpenAI |
|---|---|---|
| `application.properties` | `spring.profiles.active=ollama` | `spring.profiles.active=openai` |
| Maven build command | `mvn clean install -P ollama -DskipTests` | `mvn clean install -P openai -DskipTests` |
| API key required | ❌ No | ✅ Yes — `SPRING_AI_OPENAI_API_KEY` env var |
| Cost | Free | Pay-per-use (~$0.02/1M tokens for embeddings) |
| Internet required | ❌ No (runs locally) | ✅ Yes |
| SSL proxy issues | ❌ Not affected | ✅ Possible in corporate networks |
| Hardware needed | ~2.5 GB RAM minimum | None (cloud-based) |
| Chat model | `llama3.2` | `gpt-4o-mini` |
| Embedding model | `nomic-embed-text` | `text-embedding-3-small` |

---

## Provider Configuration Files

| File | Purpose |
|---|---|
| `application.properties` | Sets active profile — edit this to switch providers |
| `application-openai.properties` | OpenAI-specific model and key settings |
| `application-ollama.properties` | Ollama base URL, model names, keep-alive settings |
| `agentic-docs-spring-boot-starter/pom.xml` | Maven profiles that control classpath dependencies |
