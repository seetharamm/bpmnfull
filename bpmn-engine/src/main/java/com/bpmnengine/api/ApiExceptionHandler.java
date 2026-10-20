package com.bpmnengine.api;

import com.bpmnengine.engine.EngineException;
import com.bpmnengine.identity.service.IdentityException;
import com.bpmnengine.parser.BpmnParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/** Global exception handler for the REST API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EngineException.class)
    public ResponseEntity<Map<String, Object>> handleEngine(EngineException ex) {
        log.error("Engine error: {}", ex.getMessage());
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(BpmnParseException.class)
    public ResponseEntity<Map<String, Object>> handleParse(BpmnParseException ex) {
        log.error("Parse error: {}", ex.getMessage());
        return response(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IdentityException.class)
    public ResponseEntity<Map<String, Object>> handleIdentity(IdentityException ex) {
        log.warn("Identity error: {}", ex.getMessage());
        String msg = ex.getMessage();
        // Distinguish "not found" from "already exists / conflict"
        boolean notFound = msg != null && msg.toLowerCase().contains("not found");
        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return response(status, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, "Validation failed — " + errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status",    status.value(),
                "error",     status.getReasonPhrase(),
                "message",   message
        ));
    }
}
