package com.sportsbooking.dto;

import com.sportsbooking.entity.Reservation;
import com.sportsbooking.entity.Reservation.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of a Reservation returned by the API.
 */
@Schema(description = "Reservation details")
public record ReservationResponse(

        @Schema(description = "Reservation ID") Long id,
        @Schema(description = "Field ID") Long fieldId,
        @Schema(description = "Field name") String fieldName,
        @Schema(description = "Client name") String clientName,
        @Schema(description = "Client phone") String clientPhone,
        @Schema(description = "Client email") String clientEmail,
        @Schema(description = "Reservation date") LocalDate date,
        @Schema(description = "Start hour (0-23)") int startHour,
        @Schema(description = "End hour (1-24)") int endHour,
        @Schema(description = "Reservation status") Status status,
        @Schema(description = "Created at timestamp") LocalDateTime createdAt
) {
    /** Convenience factory to map from entity. */
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getField().getId(),
                r.getField().getName(),
                r.getClientName(),
                r.getClientPhone(),
                r.getClientEmail(),
                r.getDate(),
                r.getStartHour(),
                r.getEndHour(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
