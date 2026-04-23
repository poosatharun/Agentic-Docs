# Agentic Docs — Documentation Index

> **Version:** 1.0.0-SNAPSHOT  
> **Stack:** Java 21 · Spring Boot 3.4 · Spring AI 1.0.0-M6 · React 18 · Tailwind CSS v4  
> **Providers:** OpenAI (cloud) · Ollama (local/offline)

---

## Documents

| File | What it covers |
|---|---|
| [01-project-overview.md](./01-project-overview.md) | What Agentic Docs is, the problem it solves, core value proposition |
| [02-architecture.md](./02-architecture.md) | Module structure, component diagram, startup + chat data-flow |
| [03-backend-deep-dive.md](./03-backend-deep-dive.md) | Every Java class — design decisions and code walkthrough |
| [04-frontend-deep-dive.md](./04-frontend-deep-dive.md) | React component tree, UI decisions, build pipeline |
| [05-configuration-reference.md](./05-configuration-reference.md) | All properties for both providers, environment variables |
| [06-getting-started.md](./06-getting-started.md) | Step-by-step: clone → configure → run → use |
| [07-engineering-thinking.md](./07-engineering-thinking.md) | Every trade-off and decision made during the build |
| [08-api-reference.md](./08-api-reference.md) | REST API contract for the chat endpoint |
| [09-extending-the-starter.md](./09-extending-the-starter.md) | Streaming, multi-turn, auth, contributing, roadmap |
| [**10-switching-llm-providers.md**](./10-switching-llm-providers.md) | **Step-by-step guide: switch between OpenAI and Ollama** |
| [**11-ollama-local-setup.md**](./11-ollama-local-setup.md) | **Complete Ollama install + project setup on Windows (start here for offline)** |

---

## Switching Providers — TL;DR

### → Go offline (Ollama, free)

```bash
# 1. Install Ollama and pull models (one-time)
ollama pull llama3.2
ollama pull nomic-embed-text

# 2. Edit application.properties
spring.profiles.active=ollama

# 3. Rebuild and run
mvn clean install -P ollama -DskipTests
java -jar agentic-docs-sample-app/target/agentic-docs-sample-app-1.0.0-SNAPSHOT.jar
```

### → Go to cloud (OpenAI, paid)

```bash
# 1. Set API key
set SPRING_AI_OPENAI_API_KEY=sk-your-key-here   # Windows
export SPRING_AI_OPENAI_API_KEY=sk-your-key-here # macOS/Linux

# 2. Edit application.properties
spring.profiles.active=openai

# 3. Rebuild and run
mvn clean install -DskipTests
java -jar agentic-docs-sample-app/target/agentic-docs-sample-app-1.0.0-SNAPSHOT.jar
```

Full details → [10-switching-llm-providers.md](./10-switching-llm-providers.md)
