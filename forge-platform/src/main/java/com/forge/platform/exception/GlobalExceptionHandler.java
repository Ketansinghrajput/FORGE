package com.forge.platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Conflict Errors (Optimistic Locking failure ya state issues)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return buildResponse(ex.getMessage(), "CONFLICT_ERROR", HttpStatus.CONFLICT);
    }

    // 2. Bad Requests (Input validation manual fail)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(ex.getMessage(), "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    // 3. Validation Errors (DTO level @NotBlank, @Min, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationMapping(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(Map.of(
                "timestamp", LocalDateTime.now(),
                "errors", errors,
                "status", HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "code", "VALIDATION_FAILED"
        ), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // 4. Generic Fallback
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        return buildResponse(ex.getMessage(), "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper method to keep code DRY (Don't Repeat Yourself)
    private ResponseEntity<Object> buildResponse(String message, String code, HttpStatus status) {
        return new ResponseEntity<>(Map.of(
                "timestamp", LocalDateTime.now(),
                "message", message,
                "code", code,
                "status", status.value()
        ), status);
    }
}