package com.sportsbooking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error envelope returned by the GlobalExceptionHandler.
 */
@Schema(description = "Error response envelope")
public record ErrorResponse(

        @Schema(description = "HTTP status code", example = "409")
        int status,

        @Schema(description = "Short error label", example = "CONFLICT")
        String error,

        @Schema(description = "Human-readable message", example = "Time slot already booked")
        String message,

        @Schema(description = "Field-level validation errors (if any)")
        List<String> details,

        @Schema(description = "Timestamp of the error")
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, List.of(), LocalDateTime.now());
    }

    public ErrorResponse(int status, String error, String message, List<String> details) {
        this(status, error, message, details, LocalDateTime.now());
    }
}
