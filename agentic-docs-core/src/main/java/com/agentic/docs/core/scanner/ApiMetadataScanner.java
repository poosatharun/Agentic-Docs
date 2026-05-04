package com.agentic.docs.core.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Scans all {@code @RestController} endpoints on {@link ContextRefreshedEvent}
 * and publishes an {@link ApiScanCompletedEvent} for downstream ingestors.
 * Override {@link #extractDescription(Method)} to customise how descriptions are resolved.
 */
@Component
public class ApiMetadataScanner implements EndpointRepository {

    private static final Logger log = LoggerFactory.getLogger(ApiMetadataScanner.class);

    private static final List<String> INTERNAL_PACKAGE_PREFIXES = List.of(
            "com.agentic.docs.core",
            "com.agentic.docs.autoconfigure"
    );

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean scanned = new AtomicBoolean(false);
    private volatile List<ApiEndpointMetadata> scannedEndpoints = List.of();

    public ApiMetadataScanner(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                               ApplicationEventPublisher eventPublisher) {
        this.handlerMapping = handlerMapping;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Order(1)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!scanned.compareAndSet(false, true)) return;

        List<ApiEndpointMetadata> endpoints = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(e -> isUserController(e.getValue().getBeanType()))
                .map(e -> toMetadata(e.getKey(), e.getValue()))
                .toList();

        this.scannedEndpoints = List.copyOf(endpoints);
        log.info("[AgenticDocs] Scanned {} REST endpoints.", endpoints.size());
        eventPublisher.publishEvent(new ApiScanCompletedEvent(this, endpoints));
    }

    private boolean isUserController(Class<?> beanType) {
        return beanType.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class)
                && INTERNAL_PACKAGE_PREFIXES.stream().noneMatch(beanType.getName()::startsWith);
    }

    private ApiEndpointMetadata toMetadata(RequestMappingInfo info, HandlerMethod hm) {
        Class<?> beanType = hm.getBeanType();
        Method method = hm.getMethod();
        return new ApiEndpointMetadata(
                extractPath(info),
                extractHttpMethod(info),
                beanType.getSimpleName(),
                method.getName(),
                extractDescription(method),
                extractAnnotatedParams(hm, PathVariable.class),
                extractAnnotatedParams(hm, RequestParam.class),
                extractRequestBodyType(hm),
                extractResponseType(hm)
        );
    }

    /** Returns the immutable list of discovered endpoints. */
    public List<ApiEndpointMetadata> getScannedEndpoints() {
        return scannedEndpoints;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractPath(RequestMappingInfo info) {
        var patternValues = info.getPatternValues();
        if (patternValues != null && !patternValues.isEmpty()) return patternValues.iterator().next();
        var condition = info.getPathPatternsCondition();
        if (condition != null && !condition.getPatterns().isEmpty()) return condition.getPatterns().iterator().next().toString();
        return "/unknown";
    }

    private String extractHttpMethod(RequestMappingInfo info) {
        if (info.getMethodsCondition() != null && !info.getMethodsCondition().getMethods().isEmpty()) {
            return info.getMethodsCondition().getMethods().iterator().next().name();
        }
        return "GET";
    }

    /** Extracts names of parameters annotated with {@code annotationType} (PathVariable or RequestParam). */
    private <A extends java.lang.annotation.Annotation> List<String> extractAnnotatedParams(
            HandlerMethod hm, Class<A> annotationType) {
        return Arrays.stream(hm.getMethodParameters())
                .filter(mp -> mp.hasParameterAnnotation(annotationType))
                .map(mp -> resolveParamName(mp, annotationType))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private <A extends java.lang.annotation.Annotation> String resolveParamName(MethodParameter mp, Class<A> type) {
        A ann = mp.getParameterAnnotation(type);
        try {
            String value = (String) type.getMethod("value").invoke(ann);
            if (value != null && !value.isBlank()) return value;
        } catch (Exception ignored) {}
        String name = mp.getParameterName();
        return name != null ? name : "param" + mp.getParameterIndex();
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
        Method method = handlerMethod.getMethod();
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) return "void";
        if ("ResponseEntity".equals(returnType.getSimpleName())
                && method.getGenericReturnType() instanceof ParameterizedType pt
                && pt.getActualTypeArguments().length > 0) {
            String typeName = pt.getActualTypeArguments()[0].getTypeName();
            return typeName.substring(typeName.lastIndexOf('.') + 1).replace(">", "");
        }
        return returnType.getSimpleName();
    }

    /** Reads @Operation(summary/description) if SpringDoc is present, otherwise converts the method name. */
    @SuppressWarnings("unchecked")
    protected String extractDescription(Method method) {
        try {
            Class<java.lang.annotation.Annotation> opClass =
                    (Class<java.lang.annotation.Annotation>) Class.forName("io.swagger.v3.oas.annotations.Operation");
            java.lang.annotation.Annotation ann = method.getAnnotation(opClass);
            if (ann != null) {
                for (String attr : List.of("summary", "description")) {
                    String val = (String) opClass.getMethod(attr).invoke(ann);
                    if (val != null && !val.isBlank()) return val;
                }
            }
        } catch (Exception ignored) {}
        return camelToSentence(method.getName());
    }

    /**
     * Converts a camelCase identifier to a title-cased sentence.
     * <pre>
     *   "getEmployeeById"  →  "Get Employee By Id"
     *   "listOrders"       →  "List Orders"
     * </pre>
     */
    private static String camelToSentence(String name) {
        if (name == null || name.isBlank()) return name;
        String spaced = name.replaceAll("([A-Z])", " $1").trim();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
