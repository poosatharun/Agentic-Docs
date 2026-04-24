# 12 — Bug Fixes

## Overview

This document records the bugs identified during code review and the fixes applied.

---

## Fix 1 — Scanner Self-Indexing (RAG Pollution)

**File:** `agentic-docs-core/.../scanner/ApiMetadataScanner.java`  
**Severity:** Medium

**Problem:**  
`ApiMetadataScanner` was indexing its own internal endpoint (`POST /agentic-docs/api/chat`) into the vector store. This caused the LLM to occasionally surface the internal chat endpoint as if it were a user-facing API.

**Fix:**  
Added a filter to skip any `@RestController` bean whose class name starts with `com.agentic.docs`.

```java
if (beanType.getName().startsWith(SELF_PACKAGE_PREFIX)) {
    continue;
}
```

---

## Fix 2 — Thread-Safety in Scanner Duplicate Guard

**File:** `agentic-docs-core/.../scanner/ApiMetadataScanner.java`  
**Severity:** Low

**Problem:**  
The duplicate-event guard checked `scannedEndpoints.isEmpty()` on a non-`volatile` field. The JVM is free to cache the value per-thread, meaning the guard could fail under concurrent context refresh events.

**Fix:**  
Marked the field `volatile`:

```java
private volatile List<ApiEndpointMetadata> scannedEndpoints = Collections.emptyList();
```

---

## Fix 3 — Missing Input Validation in Chat Controller

**File:** `agentic-docs-core/.../chat/AgenticDocsChatController.java`  
**Severity:** Medium

**Problem:**  
A blank or `null` `question` field in the request body was passed directly to the vector store and LLM with no validation, wasting API calls and potentially causing errors.

**Fix:**  
Added an early return with `400 Bad Request` for empty questions:

```java
if (request.question() == null || request.question().isBlank()) {
    return ResponseEntity.badRequest()
            .body(new ChatResponse("Please provide a non-empty question."));
}
```

---

## Fix 4 — Null LLM Response Not Handled

**File:** `agentic-docs-core/.../chat/AgenticDocsChatController.java`  
**Severity:** Medium

**Problem:**  
`ChatClient.content()` can return `null` when the model returns an empty response (e.g., safety filters, timeouts). This `null` was returned directly to the frontend, causing a JSON serialisation issue and a broken UI state.

**Fix:**  
Added a null/blank guard with a sensible fallback:

```java
if (answer == null || answer.isBlank()) {
    answer = "I could not find a relevant endpoint for that. Please check the Swagger UI.";
}
```

The return type was also updated from `ChatResponse` to `ResponseEntity<ChatResponse>` to properly communicate HTTP status codes.

---

## Fix 5 — Broken Vertical Centering on Suggestions Screen

**File:** `agentic-docs-ui/src/App.jsx`  
**Severity:** Low

**Problem:**  
`SuggestionChips` used `flex-1` to fill available height for vertical centering, but its parent `<main>` element was not a flex container. This caused the suggestions panel to not be vertically centred on taller screens.

**Fix:**  
Added `flex flex-col` to the `<main>` element:

```jsx
<main className="flex flex-col flex-1 overflow-y-auto">
```

---

## Fix 6 — Deprecated `inline` Prop in react-markdown

**File:** `agentic-docs-ui/src/App.jsx`  
**Severity:** Low

**Problem:**  
The `code` component renderer in `react-markdown` v9 removed the `inline` prop. Using it caused a React warning and would break with future versions of the library.

**Fix:**  
Replaced the `inline` prop with a node-based heuristic that detects inline vs block code by checking for embedded newlines:

```jsx
code({ node, children, ...props }) {
  const isInline = node?.position?.start?.line === node?.position?.end?.line
    && !String(children).includes('\n');
  return isInline ? <code ...> : <div ...>
}
```

---

## Summary

| # | File | Issue | Severity |
|---|------|-------|----------|
| 1 | `ApiMetadataScanner` | Own endpoint indexed into RAG vector store | Medium |
| 2 | `ApiMetadataScanner` | Non-volatile field makes duplicate guard unreliable | Low |
| 3 | `AgenticDocsChatController` | No validation on blank/null question input | Medium |
| 4 | `AgenticDocsChatController` | Null LLM response not handled | Medium |
| 5 | `App.jsx` | `<main>` not a flex container, breaking vertical centering | Low |
| 6 | `App.jsx` | Deprecated `inline` prop in react-markdown code renderer | Low |
