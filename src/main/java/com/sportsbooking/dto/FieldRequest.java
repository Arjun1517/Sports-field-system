package com.sportsbooking.dto;

import com.sportsbooking.entity.Field.SportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Payload for creating or updating a Field (POST / PUT /api/fields).
 */
@Schema(description = "Request body for creating or updating a sports field")
public record FieldRequest(

        @NotBlank(message = "Field name is required")
        @Size(max = 100)
        @Schema(description = "Display name of the field", example = "Stade Central")
        String name,

        @NotNull(message = "Sport type is required")
        @Schema(description = "Type of sport", example = "SOCCER")
        SportType sportType,

        @NotNull(message = "Indoor flag is required")
        @Schema(description = "Whether the field is indoors", example = "false")
        Boolean indoor,

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2)
        @Schema(description = "Hourly rental price in local currency", example = "150.00")
        BigDecimal pricePerHour,

        @Schema(description = "Latitude for weather lookups (outdoor fields)", example = "33.5731")
        Double latitude,

        @Schema(description = "Longitude for weather lookups (outdoor fields)", example = "-7.5898")
        Double longitude
) {}
