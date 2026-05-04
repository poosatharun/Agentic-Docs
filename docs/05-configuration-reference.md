# 05 — Configuration Reference

## Overview

Agentic Docs uses **Ollama** as its LLM provider — free, local, and offline with no API key required.

See [11-ollama-local-setup.md](./11-ollama-local-setup.md) for installation instructions.

---

## File Layout

```
agentic-docs-sample-app/src/main/resources/
├── application.properties          ← Shared settings
└── application-ollama.properties   ← Ollama config (local/offline)
```

---

## `application.properties` — Shared Settings

```properties
server.port=8080

# ── Active LLM Profile ────────────────────────────────────────────────────────
# openai → Uses OpenAI API (requires SPRING_AI_OPENAI_API_KEY env var)
# ollama → Uses local Ollama (requires ollama serve running on localhost:11434)
spring.profiles.active=openai

# ── Agentic Docs ──────────────────────────────────────────────────────────────
# Enabled by default (matchIfMissing=true). Set to false to disable.
# agentic.docs.enabled=false

# ── Logging ───────────────────────────────────────────────────────────────────
logging.level.com.agentic.docs=INFO
```

### Agentic Docs Properties

| Property | Type | Default | Required | Description |
|---|---|---|---|---|
| `agentic.docs.enabled` | `boolean` | `true` | No | Master on/off switch for the starter (enabled by default) |
| `spring.profiles.active` | `string` | — | **Yes** | Set to `openai` or `ollama` |

---

## `application-ollama.properties` — Ollama Provider

Activated by `spring.profiles.active=ollama` in `application.properties` (the default).

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text

# Performance tuning
spring.ai.ollama.chat.options.keep-alive=-1
spring.ai.ollama.chat.options.num-ctx=2048
spring.ai.ollama.chat.options.num-predict=512
spring.ai.ollama.chat.options.temperature=0.1
```

### Ollama Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `spring.ai.ollama.base-url` | `String` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `String` | — | Chat model name (must be pulled first) |
| `spring.ai.ollama.embedding.options.model` | `String` | — | Embedding model name (must be pulled first) |
| `spring.ai.ollama.chat.options.keep-alive` | `String` | `-1` | How long to keep model in RAM. `-1` = indefinitely (no cold-start). Set to e.g. `10m` if RAM is constrained. |
| `spring.ai.ollama.chat.options.num-ctx` | `int` | `4096` | Context window size in tokens. `2048` is sufficient for RAG API docs and halves inference overhead. |
| `spring.ai.ollama.chat.options.num-predict` | `int` | unlimited | Max tokens to generate per response. `512` (~350 words) is ample for any API answer. |
| `spring.ai.ollama.chat.options.temperature` | `float` | `0.8` | Lower = more deterministic. `0.1` improves factual accuracy for API documentation. |

### Recommended Ollama Models

| Use Case | Model | RAM Required | Pull Command |
|---|---|---|---|
| Chat (small, fast) ✓ | `llama3.2` | ~2 GB | `ollama pull llama3.2` |
| Chat (better quality) | `llama3.1:8b` | ~5 GB | `ollama pull llama3.1:8b` |
| Chat (best quality) | `llama3.3:70b` | ~40 GB | `ollama pull llama3.3:70b` |
| Embeddings ✓ | `nomic-embed-text` | ~274 MB | `ollama pull nomic-embed-text` |
| Embeddings (alternative) | `mxbai-embed-large` | ~670 MB | `ollama pull mxbai-embed-large` |

---

## Environment Variables

| Variable | Used by | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | All | Override active profile without editing files |

### Setting environment variables

**Windows CMD:**
```cmd
set SPRING_PROFILES_ACTIVE=ollama
```

**Windows PowerShell:**
```powershell
$env:SPRING_PROFILES_ACTIVE = "ollama"
```

**macOS / Linux:**
```bash
export SPRING_PROFILES_ACTIVE=ollama
```

**Docker Compose:**
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=ollama
```
