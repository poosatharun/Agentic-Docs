package com.agentic.docs.core.model;

/**
 * Response body returned by the {@code POST /agentic-docs/api/chat} endpoint.
 *
 * @param answer the LLM-generated answer (or a friendly fallback message)
 */
public record ChatResponse(String answer) {}
