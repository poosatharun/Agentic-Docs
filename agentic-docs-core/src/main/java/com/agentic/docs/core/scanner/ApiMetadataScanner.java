package com.agentic.docs.core.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listens for {@link ContextRefreshedEvent} and uses {@link RequestMappingHandlerMapping}
 * to discover every {@code @RestController} endpoint in the host application.
 *
 * <p>Swagger {@code @Operation} summaries are read reflectively so that the core module
 * does not have a hard compile-time dependency on springdoc.</p>
 */
@Component
public class ApiMetadataScanner implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ApiMetadataScanner.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private List<ApiEndpointMetadata> scannedEndpoints = Collections.emptyList();

    public ApiMetadataScanner(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Guard against duplicate events fired in parent/child contexts
        if (!scannedEndpoints.isEmpty()) return;

        List<ApiEndpointMetadata> endpoints = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Only process @RestController beans
            Class<?> beanType = handlerMethod.getBeanType();
            if (!beanType.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)) {
                continue;
            }

            String path = extractPath(mappingInfo);
            String httpMethod = extractHttpMethod(mappingInfo);
            String controllerName = beanType.getSimpleName();
            String methodName = handlerMethod.getMethod().getName();
            String description = extractDescription(handlerMethod.getMethod());

            endpoints.add(new ApiEndpointMetadata(path, httpMethod, controllerName, methodName, description));
        }

        this.scannedEndpoints = Collections.unmodifiableList(endpoints);
        log.info("[AgenticDocs] Scanned {} REST endpoints for RAG indexing.", endpoints.size());
    }

    /** Returns the immutable list of discovered endpoints. */
    public List<ApiEndpointMetadata> getScannedEndpoints() {
        return scannedEndpoints;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractPath(RequestMappingInfo info) {
        if (info.getPatternValues() != null && !info.getPatternValues().isEmpty()) {
            return info.getPatternValues().iterator().next();
        }
        // Fallback for PathPatternParser-based mappings
        if (info.getPathPatternsCondition() != null) {
            Set<?> patterns = info.getPathPatternsCondition().getPatterns();
            if (!patterns.isEmpty()) return patterns.iterator().next().toString();
        }
        return "/unknown";
    }

    private String extractHttpMethod(RequestMappingInfo info) {
        if (info.getMethodsCondition() != null && !info.getMethodsCondition().getMethods().isEmpty()) {
            return info.getMethodsCondition().getMethods().iterator().next().name();
        }
        return "GET";
    }

    /**
     * Reads the {@code @Operation(summary = "...")} annotation reflectively.
     * Falls back to the method name if springdoc is not on the classpath.
     */
    private String extractDescription(Method method) {
        try {
            Class<?> operationAnnotation = Class.forName("io.swagger.v3.oas.annotations.Operation");
            Object annotation = method.getAnnotation(
                    (Class<java.lang.annotation.Annotation>) operationAnnotation);
            if (annotation != null) {
                String summary = (String) operationAnnotation.getMethod("summary").invoke(annotation);
                if (summary != null && !summary.isBlank()) return summary;
                String desc = (String) operationAnnotation.getMethod("description").invoke(annotation);
                if (desc != null && !desc.isBlank()) return desc;
            }
        } catch (Exception ignored) {
            // springdoc not on classpath — that's fine
        }
        return "No description provided.";
    }
}
