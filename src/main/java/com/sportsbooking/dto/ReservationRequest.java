package com.sportsbooking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Payload for POST /api/reservations.
 * Hours are integers 0-23 for start, 1-24 for end (startHour < endHour).
 */
@Schema(description = "Request body for creating a reservation")
public record ReservationRequest(

        @NotNull(message = "Field ID is required")
        @Schema(description = "ID of the field to reserve", example = "1")
        Long fieldId,

        @NotBlank(message = "Client name is required")
        @Size(max = 100)
        @Schema(description = "Full name of the client", example = "Youssef Alami")
        String clientName,

        @NotBlank(message = "Client phone is required")
        @Pattern(regexp = "^[+0-9\\-\\s]{6,20}$", message = "Invalid phone number format")
        @Schema(description = "Client phone number", example = "+212 6 12 34 56 78")
        String clientPhone,

        @NotBlank(message = "Client email is required")
        @Email(message = "Invalid email address")
        @Schema(description = "Client email address", example = "youssef@example.com")
        String clientEmail,

        @NotNull(message = "Reservation date is required")
        @FutureOrPresent(message = "Reservation date must not be in the past")
        @Schema(description = "Date of the reservation (ISO-8601)", example = "2026-09-15")
        LocalDate date,

        @Min(value = 0, message = "Start hour must be between 0 and 23")
        @Max(value = 23, message = "Start hour must be between 0 and 23")
        @Schema(description = "Start hour (0-23)", example = "9")
        int startHour,

        @Min(value = 1, message = "End hour must be between 1 and 24")
        @Max(value = 24, message = "End hour must be between 1 and 24")
        @Schema(description = "End hour (1-24, exclusive)", example = "11")
        int endHour
) {}
