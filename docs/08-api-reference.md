# 08 — API Reference

## Chat Endpoint

The only public API endpoint exposed by Agentic Docs.

---

### `POST /agentic-docs/api/chat`

Accepts a natural language question, performs RAG retrieval against the indexed endpoints, and returns an LLM-generated answer.

#### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "question": "string"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `question` | `string` | Yes | The natural language question from the developer |

**Example:**
```json
{
  "question": "How do I cancel a subscription with a partial refund?"
}
```

---

#### Response

**Status:** `200 OK`

**Body:**
```json
{
  "answer": "string"
}
```

| Field | Type | Description |
|---|---|---|
| `answer` | `string` | The LLM-generated answer, formatted as Markdown |

**Example:**
```json
{
  "answer": "To cancel a subscription with a partial refund, you need to call the **Termination API**.\n\n## Steps\n\n1. First, check the subscription's `daysActive` field:\n\n```java\nSubscription sub = subscriptionClient.getDetails(subId);\nboolean isEligible = sub.getDaysActive() < 15;\n```\n\n2. Then call `POST /api/v1/subscriptions/{id}/terminate` with:\n\n```json\n{\n  \"refundType\": \"PARTIAL\",\n  \"isProrated\": true\n}\n```"
}
```

---

#### Error Responses

| Status | Cause | Body |
|---|---|---|
| `400 Bad Request` | Missing or null `question` field | Spring default error body |
| `500 Internal Server Error` | OpenAI API failure or vector store error | Spring default error body |
| `503 Service Unavailable` | OpenAI rate limit exceeded | Spring default error body |

---

#### CORS

The endpoint has `@CrossOrigin(origins = "*")` applied. All origins are allowed. This enables:
- The Vite dev server (port 5173) to call the Spring Boot backend (port 8080)
- Any frontend application to use the API

For production, replace `"*"` with your specific origin in `AgenticDocsChatController`.

---

## Example `curl` Calls

### Basic question
```bash
curl -X POST http://localhost:8080/agentic-docs/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What endpoints are available?"}'
```

### Code generation request
```bash
curl -X POST http://localhost:8080/agentic-docs/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "Generate Java code to process a payment"}'
```

### Multi-step workflow question
```bash
curl -X POST http://localhost:8080/agentic-docs/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I cancel a premium subscription but only trigger a partial refund if the user has been active for less than 15 days?"}'
```

---

## Static UI Resources

These are served by Spring Boot's `ResourceHttpRequestHandler` — not REST endpoints.

| URL | Description |
|---|---|
| `GET /agentic-docs/` | React SPA entry point (index.html) |
| `GET /agentic-docs/index.html` | Same as above |
| `GET /agentic-docs/assets/*.js` | Vite-built JavaScript bundle |
| `GET /agentic-docs/assets/*.css` | Vite-built CSS bundle |

---

## Internal Data Contracts

These are not HTTP endpoints but document the internal Java records used by the chat controller.

### `ChatRequest`
```java
public record ChatRequest(String question) {}
```

### `ChatResponse`
```java
public record ChatResponse(String answer) {}
```

### `ApiEndpointMetadata`
```java
public record ApiEndpointMetadata(
    String path,          // e.g. "/api/v1/subscriptions/{id}"
    String httpMethod,    // e.g. "POST"
    String controllerName, // e.g. "PaymentsController"
    String methodName,    // e.g. "terminateSubscription"
    String description    // e.g. "Terminate a subscription..."
) {}
```
