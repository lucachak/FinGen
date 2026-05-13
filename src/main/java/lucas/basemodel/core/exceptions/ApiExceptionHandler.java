package lucas.basemodel.core.exceptions;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for /api/v1/** routes.
 * Returns consistent JSON error responses to the Flutter client.
 *
 * Error response format:
 * {
 * "status": 400,
 * "error": "Validation failed",
 * "timestamp": "2024-08-01T10:30:00",
 * "fields": { "email": "must not be blank" } // only for validation errors
 * }
 */
@RestControllerAdvice(basePackages = "lucas.basemodel.web.api")
public class ApiExceptionHandler {

    // ── Validation errors (@Valid failures) ───────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"));

        return ResponseEntity.badRequest().body(
                ApiError.builder()
                        .status(400)
                        .error("Validation failed")
                        .timestamp(LocalDateTime.now())
                        .fields(fields)
                        .build());
    }

    // ── Resource not found ────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.builder()
                        .status(404)
                        .error(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ── Unauthorized access (user trying to access another user's data) ───────
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.builder()
                        .status(403)
                        .error(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.builder()
                        .status(500)
                        .error("Internal server error")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ── ApiError DTO ──────────────────────────────────────────────────────────
    @Data
    @Builder
    public static class ApiError {
        private int status;
        private String error;
        private LocalDateTime timestamp;
        private Map<String, String> fields; // nullable, only for validation errors
    }
}

// ─── ResourceNotFoundException.java ──────────────────────────────────────────
// public class ResourceNotFoundException extends RuntimeException {
// public ResourceNotFoundException(String message) { super(message); }
// }

// ─── UnauthorizedAccessException.java ────────────────────────────────────────
// public class UnauthorizedAccessException extends RuntimeException {
// public UnauthorizedAccessException(String message) { super(message); }
// }