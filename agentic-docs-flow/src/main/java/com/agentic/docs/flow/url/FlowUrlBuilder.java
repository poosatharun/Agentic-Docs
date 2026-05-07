package com.agentic.docs.flow.url;

import com.agentic.docs.flow.model.FlowRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds the full target URL from a {@link FlowRequest}, substituting any
 * path-parameter tokens. Extracted from {@code FlowExecutorService} (SRP).
 */
@Component
public class FlowUrlBuilder {

    private final int serverPort;

    public FlowUrlBuilder(@Value("${server.port:8080}") int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Resolve all {@code {param}} tokens in the request path and prepend the
     * local base URL.
     *
     * @param request the incoming flow request
     * @return a fully-qualified URL such as {@code http://localhost:8080/api/v1/checkout}
     */
    public String build(FlowRequest request) {
        String path = request.path();

        if (request.pathParams() != null) {
            for (Map.Entry<String, String> entry : request.pathParams().entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = (entry.getValue() != null && !entry.getValue().isBlank())
                        ? entry.getValue()
                        : "_";
                path = path.replace(placeholder, value);
            }
        }

        return "http://localhost:" + serverPort + path;
    }
}
