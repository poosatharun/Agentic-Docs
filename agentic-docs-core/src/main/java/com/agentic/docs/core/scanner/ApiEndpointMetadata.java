package com.agentic.docs.core.scanner;

/**
 * DTO to hold the extracted API details.
 * This will be converted into a Spring AI Document for vectorization.
 */
public record ApiEndpointMetadata(
        String path,
        String httpMethod,
        String controllerName,
        String methodName,
        String description
) {
    /**
     * Converts the metadata into a plain text string suitable for an LLM to read.
     */
    public String toLlmReadableText() {
        return String.format(
            "API Endpoint: [%s] %s\n" +
            "Controller: %s\n" +
            "Java Method: %s\n" +
            "Description: %s\n",
            httpMethod, path, controllerName, methodName, description
        );
    }
}