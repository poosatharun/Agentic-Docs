# Contributing to Agentic Docs

Thank you for your interest in contributing! This guide explains everything you need to know to get started — whether you're fixing a typo, reporting a bug, or adding a new feature.

---

## 📋 Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [How to Report a Bug](#how-to-report-a-bug)
3. [How to Request a Feature](#how-to-request-a-feature)
4. [Development Setup](#development-setup)
5. [Branch & Commit Conventions](#branch--commit-conventions)
6. [Submitting a Pull Request](#submitting-a-pull-request)
7. [Project Structure Quick Reference](#project-structure-quick-reference)
8. [Versioning Policy](#versioning-policy)

---

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).  
By participating, you agree to uphold a welcoming and respectful environment.

---

## How to Report a Bug

1. Search [existing issues](https://github.com/poosatharun/Agentic-Docs/issues) first — it may already be reported.
2. If not, open a new issue using the **Bug Report** template.
3. Include: what you expected, what actually happened, and steps to reproduce.

---

## How to Request a Feature

1. Open an issue using the **Feature Request** template.
2. Describe the problem you're solving and why it belongs in this library (not in the host app).

---

## Development Setup

### Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 20 |
| npm | 10 |

### 1. Clone and build the backend

```bash
git clone https://github.com/poosatharun/Agentic-Docs.git
cd Agentic-Docs

# Build all modules and run all tests
mvn verify
```

### 2. Run the sample app (OpenAI)

```bash
# Export your OpenAI API key
export SPRING_AI_OPENAI_API_KEY=sk-...

# Run the sample app
cd agentic-docs-sample-app
mvn spring-boot:run
```

Then open http://localhost:8080/agentic-docs/ in your browser.

### 3. Run the sample app (Ollama — free, local)

```bash
# Install and start Ollama: https://ollama.com
ollama pull nomic-embed-text
ollama pull llama3.2

# Run with the ollama profile
cd agentic-docs-sample-app
mvn spring-boot:run -Dspring-boot.run.profiles=ollama -P ollama
```

### 4. Run the UI in development mode

```bash
cd agentic-docs-ui
npm install
npm run dev   # Starts Vite dev server on http://localhost:5173
```

The Vite dev server proxies API calls to the running Spring Boot app.

---

## Branch & Commit Conventions

### Branches

| Branch | Purpose |
|--------|---------|
| `main` | Stable, released code |
| `develop` | Integration branch — all feature branches merge here first |
| `feature/<short-description>` | New features (e.g. `feature/streaming-chat`) |
| `fix/<short-description>` | Bug fixes (e.g. `fix/cors-wildcard`) |
| `docs/<short-description>` | Documentation only changes |

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

Examples:
feat(chat): add streaming response support
fix(cors): replace wildcard with configurable origins
docs(readme): add Ollama setup instructions
test(ingestor): add idempotency test for ApiDocumentIngestor
refactor(scanner): extract reflective description reading into helper
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`

---

## Submitting a Pull Request

1. **Fork** the repository and create your branch from `develop`.
2. **Write tests** for any new logic (see `agentic-docs-core/src/test/`).
3. **Run the full build** locally before pushing: `mvn verify`
4. **Open a PR** against the `develop` branch (not `main`).
5. Fill in the PR template — describe what changed and why.
6. CI will run automatically. Fix any failures before requesting review.

---

## Project Structure Quick Reference

```
agentic-docs-core/               ← All business logic lives here
  src/main/java/.../core/
    chat/
      AgenticDocsChatController  ← Thin HTTP layer (reads request, calls service)
      AgenticDocsChatService     ← RAG pipeline + LLM call (the real work)
    config/
      AgenticDocsProperties      ← All configurable settings (topK, systemPrompt, CORS)
      VectorStoreConfig          ← In-memory vector store (users can swap for Pgvector etc.)
      AgenticDocsMvcConfigurer   ← CORS + UI forwarding rules
    ingestor/
      ApiDocumentIngestor        ← Converts endpoint metadata → vector store documents
    model/
      ChatRequest / ChatResponse ← Simple DTOs (data transfer objects)
    scanner/
      ApiMetadataScanner         ← Discovers @RestController endpoints on startup
      ApiEndpointMetadata        ← Immutable record holding one endpoint's data

agentic-docs-spring-boot-starter/ ← Autoconfiguration entry point
  AgenticDocsAutoConfiguration    ← Enables properties + scans core beans

agentic-docs-sample-app/          ← Example host app to test the starter
agentic-docs-ui/                  ← React + Vite frontend (built into the starter JAR)
```

---

## Versioning Policy

This project uses [Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`

| Increment | When |
|-----------|------|
| `PATCH` | Bug fix, no API change (`1.0.1`) |
| `MINOR` | New feature, backwards compatible (`1.1.0`) |
| `MAJOR` | Breaking API change (`2.0.0`) |

Current version: see the `<version>` tag in the root `pom.xml`.
