package com.sportsbooking.exception;

import com.sportsbooking.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Central exception → HTTP response mapper.
 *
 * All error bodies follow the {@link ErrorResponse} record:
 * {
 *   "status":    <http code>,
 *   "error":     "<label>",
 *   "message":   "<human-readable>",
 *   "details":   [ ... field-level errors ... ],
 *   "timestamp": "..."
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 400 Bad Request ───────────────────────────────────────────────────────

    /** Bean Validation failures from @Valid on request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .toList();

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Request validation failed", details);
        return ResponseEntity.badRequest().body(body);
    }

    /** e.g. wrong type for a path variable or request param. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Parameter '%s' has invalid value '%s'",
                ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", msg));
    }

    /** startHour >= endHour passed the annotation check. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage()));
    }

    // ─── 401 Unauthorized ──────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed login attempt: bad credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Invalid username or password"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Account is disabled"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Authentication failed: " + ex.getMessage()));
    }

    // ─── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage()));
    }

    // ─── 409 Conflict ──────────────────────────────────────────────────────────

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ReservationConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage()));
    }

    // ─── 500 Internal Server Error (catch-all) ─────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. Please try again later."));
    }
}
