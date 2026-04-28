# Agentic Docs

**Agentic Docs** is a Spring Boot starter that turns static API documentation (like Swagger) into an interactive AI Agent.

---

## Table of Contents
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Setup & Running](#setup--running)
  - [Option 1 — OpenAI (Cloud)](#option-1--openai-cloud)
  - [Option 2 — Ollama (Local, Free)](#option-2--ollama-local-free)
- [Running the Sample App](#running-the-sample-app)
- [Running the UI (Development)](#running-the-ui-development)
- [Accessing the Agent](#accessing-the-agent)
- [Configuration Reference](#configuration-reference)

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | [Download](https://adoptium.net/) |
| Maven | 3.9+ | [Download](https://maven.apache.org/download.cgi) |
| Node.js | 18+ | Only needed for UI development |
| Ollama | Latest | Only needed for local LLM — [Download](https://ollama.com/download) |

---

## Project Structure

```
agentic-docs/
├── agentic-docs-core/              # Core RAG scanning & chat logic
├── agentic-docs-spring-boot-starter/  # Auto-configuration & bundled UI
├── agentic-docs-sample-app/        # Runnable demo application
└── agentic-docs-ui/                # React frontend source (Vite)
```

---

## Setup & Running

### 1. Clone the Repository

```bash
git clone https://github.com/poosatharun/Agentic-Docs.git
cd Agentic-Docs
```

### 2. Build the Project

```bash
mvn clean install -DskipTests
```

This builds all modules in order: `core` → `spring-boot-starter` → `sample-app`.

---

### Option 1 — OpenAI (Cloud)

> Requires an OpenAI API key. Approx. cost: **< $0.01 per session**.

**Step 1:** Set your API key as an environment variable.

- **Windows (PowerShell):**
  ```powershell
  $env:SPRING_AI_OPENAI_API_KEY="sk-your-key-here"
  ```
- **macOS/Linux:**
  ```bash
  export SPRING_AI_OPENAI_API_KEY=sk-your-key-here
  ```

**Step 2:** Make sure `application.properties` has:
```properties
spring.profiles.active=openai
```

**Step 3:** Run the sample app:
```bash
cd agentic-docs-sample-app
mvn spring-boot:run
```

---

### Option 2 — Ollama (Local, Free)

> Runs entirely on your machine — no API key, no cost.

**Step 1:** Install Ollama from [https://ollama.com/download](https://ollama.com/download)

**Step 2:** Pull the required models (one-time setup):
```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

**Step 3:** Start Ollama:
```bash
ollama serve
```

**Step 4:** Set the active profile in `agentic-docs-sample-app/src/main/resources/application.properties`:
```properties
spring.profiles.active=ollama
```

**Step 5:** Run the sample app:
```bash
cd agentic-docs-sample-app
mvn spring-boot:run
```

---

## Running the Sample App

Once started, the backend runs on **http://localhost:8080**.

You can also pass the profile at runtime without editing any files:

```bash
# OpenAI
mvn spring-boot:run -Dspring-boot.run.profiles=openai

# Ollama
mvn spring-boot:run -Dspring-boot.run.profiles=ollama
```

---

## Running the UI (Development)

The starter ships with a **pre-built UI** embedded in the JAR. For UI development only:

```bash
cd agentic-docs-ui
npm install
npm run dev
```

The dev UI runs on **http://localhost:5173** and proxies API calls to `http://localhost:8080`.

To build and bundle the UI into the starter:
```bash
npm run build
```

---

## Accessing the Agent

| URL | Description |
|-----|-------------|
| `http://localhost:8080/agentic-docs` | Agentic Docs chat UI |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (sample app) |
| `http://localhost:8080/agentic-docs/api/chat` | RAG chat REST endpoint |

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `spring.profiles.active` | `openai` | LLM provider: `openai` or `ollama` |
| `agentic.docs.enabled` | `true` | Enable/disable the RAG agent |
| `SPRING_AI_OPENAI_API_KEY` | *(env var)* | Your OpenAI API key |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.openai.chat.options.model` | `gpt-4o-mini` | OpenAI chat model |
| `spring.ai.ollama.chat.options.model` | `llama3.2` | Ollama chat model |

---



### What is it?
Standard API docs are just lists of endpoints. This tool embeds a **RAG (Retrieval-Augmented Generation) Agent** directly into your browser. Instead of searching through long tables, you can ask the documentation questions in plain English.

### The Core Idea
If a developer wants to know:
> *"How do I implement a partial refund using these APIs?"*

The agent reads your project's logic and generates the specific **React** or **Java** code snippets needed to make it work.

###  Tech Stack
* **Backend:** Java 21 & Spring Boot 3
* **AI:** Spring AI (for RAG and LLM orchestration)
* **Frontend:** React (for the chat UI)
* **Database:** Vector Store (to index your API endpoints)


### Why use this?
In large companies, developers waste hours trying to understand complex internal APIs. This project solves that by creating a "Pair Programmer" that lives inside your documentation.

---
*Built for the 2026 Open Source Community.*





# Use Case: Streamlining Microservice Integration

###  The Environment
Imagine a senior developer at working on a "Payments & Ledger" microservice. The service has over **150+ endpoints**, complex DTO inheritance, and legacy documentation that hasn't been updated in months.

### The Problem (Traditional Workflow)
A new developer needs to implement a **Conditional Subscription Cancellation**.
1. **Search:** They open Swagger and search for "cancel." They find 5 different endpoints.
2. **Analysis:** They spend 45 minutes reading the schema for each to see which one handles "Refund Eligibility."
3. **Trial & Error:** They manually build a JSON payload, but the API returns a `400 Bad Request` because a hidden header was required.
4. **Outcome:** 3 hours wasted on research and debugging.

---

### The Solution (Agentic Docs Workflow)
The developer opens the `/agentic-docs` portal powered by your Spring Boot starter.

#### 1. Contextual Inquiry
The developer types into the embedded chat:
> *"How do I cancel a premium subscription but only trigger a partial refund if the user has been active for less than 15 days?"*

#### 2. Agentic Reasoning
The **Agentic Docs** engine performs the following:
* **Vector Search:** Finds the `/subscriptions/{id}/terminate` and `/billing/refund/calculate` endpoints.
* **Code Analysis:** Scans the `@Service` layer to see that the `CancellationRequest` DTO requires an `isProrated` boolean.
* **Logic Synthesis:** Realizes that a "15-day check" is a business logic requirement often handled by the `AccountService`.

#### 3. Instant Implementation
The agent responds with a step-by-step guide and code:
> "To handle this, you need to call the **Termination API** followed by the **Refund API**. Here is your implementation logic:"

```java
// Generated by Agentic Docs Agent
public void handleSmartCancellation(String subId) {
    Subscription sub = subscriptionClient.getDetails(subId);
    
    // Logic based on your 15-day requirement
    boolean isEligible = sub.getDaysActive() < 15;
    
    TerminationRequest request = new TerminationRequest();
    request.setRefundType(isEligible ? "PARTIAL" : "NONE");
    
    apiClient.terminateSubscription(subId, request);
}
