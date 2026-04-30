package com.agentic.docs.core.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Listens for {@link ContextRefreshedEvent} and uses {@link RequestMappingHandlerMapping}
 * to discover every {@code @RestController} endpoint in the host application.
 */
@Component
public class ApiMetadataScanner {

    private static final Logger log = LoggerFactory.getLogger(ApiMetadataScanner.class);

    private static final List<String> INTERNAL_PACKAGE_PREFIXES = List.of(
            "com.agentic.docs.core",
            "com.agentic.docs.autoconfigure"
    );

    private final RequestMappingHandlerMapping handlerMapping;
    private final AtomicBoolean scanned = new AtomicBoolean(false);
    private volatile List<ApiEndpointMetadata> scannedEndpoints = Collections.emptyList();

    public ApiMetadataScanner(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order(1)  // Must run BEFORE ApiDocumentIngestor (@Order(2))
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Guard against duplicate events fired in parent/child contexts
        if (!scanned.compareAndSet(false, true)) return;

        List<ApiEndpointMetadata> endpoints = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Only process @RestController beans; skip Agentic Docs' own internal endpoints
            Class<?> beanType = handlerMethod.getBeanType();
            if (!beanType.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)) {
                continue;
            }
            String beanClassName = beanType.getName();
            if (INTERNAL_PACKAGE_PREFIXES.stream().anyMatch(beanClassName::startsWith)) {
                continue;
            }

            String path = extractPath(mappingInfo);
            String httpMethod = extractHttpMethod(mappingInfo);
            String controllerName = beanType.getSimpleName();
            String methodName = handlerMethod.getMethod().getName();
            String description = extractDescription(handlerMethod.getMethod());
            List<String> pathParams = extractPathParams(handlerMethod);
            List<String> queryParams = extractQueryParams(handlerMethod);
            String requestBodyType = extractRequestBodyType(handlerMethod);
            String responseType = extractResponseType(handlerMethod);

            endpoints.add(new ApiEndpointMetadata(path, httpMethod, controllerName, methodName,
                    description, pathParams, queryParams, requestBodyType, responseType));
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

    private List<String> extractPathParams(HandlerMethod handlerMethod) {
        List<String> params = new ArrayList<>();
        for (MethodParameter mp : handlerMethod.getMethodParameters()) {
            PathVariable pv = mp.getParameterAnnotation(PathVariable.class);
            if (pv != null) {
                String name = (pv.value() != null && !pv.value().isBlank()) ? pv.value() : mp.getParameterName();
                params.add(name != null ? name : "param" + mp.getParameterIndex());
            }
        }
        return Collections.unmodifiableList(params);
    }

    private List<String> extractQueryParams(HandlerMethod handlerMethod) {
        List<String> params = new ArrayList<>();
        for (MethodParameter mp : handlerMethod.getMethodParameters()) {
            RequestParam rp = mp.getParameterAnnotation(RequestParam.class);
            if (rp != null) {
                String name = (rp.value() != null && !rp.value().isBlank()) ? rp.value() : mp.getParameterName();
                params.add(name != null ? name : "param" + mp.getParameterIndex());
            }
        }
        return Collections.unmodifiableList(params);
    }

    private String extractRequestBodyType(HandlerMethod handlerMethod) {
        for (MethodParameter mp : handlerMethod.getMethodParameters()) {
            if (mp.hasParameterAnnotation(RequestBody.class)) {
                return mp.getParameterType().getSimpleName();
            }
        }
        return null;
    }

    private String extractResponseType(HandlerMethod handlerMethod) {
        Class<?> returnType = handlerMethod.getMethod().getReturnType();
        if (returnType == void.class || returnType == Void.class) return "void";
        if ("ResponseEntity".equals(returnType.getSimpleName())) {
            Type genericReturn = handlerMethod.getMethod().getGenericReturnType();
            if (genericReturn instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) {
                    String typeName = args[0].getTypeName();
                    // strip package prefix and any trailing >
                    int lastDot = typeName.lastIndexOf('.');
                    return typeName.substring(lastDot + 1).replace(">", "");
                }
            }
        }
        return returnType.getSimpleName();
    }

    /**
     * Returns the method name as a human-readable description.
     * Override this method to plug in a custom description source.
     */
    private String extractDescription(Method method) {
        return method.getName();
    }
}
