# 05 — Configuration Reference

## Overview

Agentic Docs supports two LLM providers out of the box:

| Provider | Mode | Cost | Requires |
|---|---|---|---|
| **OpenAI** | Cloud (online) | ~$0.01/session | API key + internet |
| **Ollama** | Local (offline) | Free | Ollama installed + GPU/CPU |

Switching between them is a **two-step process**:
1. Change one line in `application.properties`
2. Rebuild with the matching Maven profile flag

See [10-switching-llm-providers.md](./10-switching-llm-providers.md) for the full step-by-step guide.

---

## File Layout

```
agentic-docs-sample-app/src/main/resources/
├── application.properties          ← Shared settings + active profile selector
├── application-openai.properties   ← OpenAI-specific config (cloud)
└── application-ollama.properties   ← Ollama-specific config (local/offline)
```

Spring Boot automatically loads `application-{profile}.properties` when
`spring.profiles.active={profile}` is set in `application.properties`.

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

## `application-openai.properties` — OpenAI Provider

Loaded automatically when `spring.profiles.active=openai`.

```properties
spring.ai.openai.api-key=${SPRING_AI_OPENAI_API_KEY:your-openai-api-key-here}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.embedding.options.model=text-embedding-3-small
```

### OpenAI Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `spring.ai.openai.api-key` | `String` | — | API key. Use env var `SPRING_AI_OPENAI_API_KEY` |
| `spring.ai.openai.chat.options.model` | `String` | `gpt-4o` | Chat model name |
| `spring.ai.openai.embedding.options.model` | `String` | `text-embedding-ada-002` | Embedding model name |
| `spring.ai.openai.base-url` | `String` | `https://api.openai.com` | Override for Azure or proxies |

### Recommended OpenAI Models

| Use Case | Model | Notes |
|---|---|---|
| Best quality | `gpt-4o` | Slower, more expensive |
| Best value ✓ | `gpt-4o-mini` | Recommended default |
| Embeddings ✓ | `text-embedding-3-small` | Recommended default |
| Embeddings (high quality) | `text-embedding-3-large` | 3× more expensive |

---

## `application-ollama.properties` — Ollama Provider

Loaded automatically when `spring.profiles.active=ollama`.

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text
spring.ai.ollama.chat.options.keep-alive=5m
```

### Ollama Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `spring.ai.ollama.base-url` | `String` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.options.model` | `String` | — | Chat model name (must be pulled first) |
| `spring.ai.ollama.embedding.options.model` | `String` | — | Embedding model name (must be pulled first) |
| `spring.ai.ollama.chat.options.keep-alive` | `String` | `5m` | How long to keep model loaded in memory |

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
| `SPRING_AI_OPENAI_API_KEY` | OpenAI profile | Your OpenAI secret key (`sk-...`) |
| `SPRING_PROFILES_ACTIVE` | Both | Override active profile without editing files |

### Setting environment variables

**Windows CMD:**
```cmd
set SPRING_AI_OPENAI_API_KEY=sk-your-key-here
```

**Windows PowerShell:**
```powershell
$env:SPRING_AI_OPENAI_API_KEY = "sk-your-key-here"
```

**macOS / Linux:**
```bash
export SPRING_AI_OPENAI_API_KEY=sk-your-key-here
```

**Docker Compose:**
```yaml
environment:
  - SPRING_AI_OPENAI_API_KEY=sk-your-key-here
  - SPRING_PROFILES_ACTIVE=openai
```
