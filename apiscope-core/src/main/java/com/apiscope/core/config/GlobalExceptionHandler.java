package com.apiscope.core.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for APIScope REST endpoints.
 * Converts validation errors into clean JSON 400 responses instead of Spring's default HTML error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Validated failures on @RequestBody (e.g. blank question in ChatRequest).
     * Returns: { "error": "Question must not be blank" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    /**
     * Handles unexpected runtime errors in chat/stream endpoints.
     * Returns: { "error": "<message>" } with HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericErrors(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred: " + ex.getMessage()));
    }
}
