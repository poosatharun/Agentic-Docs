package com.agentic.docs.core.scanner;

/**
 * Immutable DTO representing a single REST endpoint discovered in the host application.
 * The {@link #toLlmReadableText()} method produces the text that gets embedded into the vector store.
 */
public record ApiEndpointMetadata(
        String path,
        String httpMethod,
        String controllerName,
        String methodName,
        String description
) {
    /**
     * Produces a structured plain-text representation suitable for LLM context injection.
     */
    public String toLlmReadableText() {
        return """
                Endpoint  : [%s] %s
                Controller: %s
                Method    : %s
                Summary   : %s
                """.formatted(httpMethod, path, controllerName, methodName, description);
    }
}
