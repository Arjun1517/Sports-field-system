package com.sportsbooking.dto;

import com.sportsbooking.entity.Field;
import com.sportsbooking.entity.Field.SportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Read-only view of a Field returned by the API.
 */
@Schema(description = "Sports field details")
public record FieldResponse(

        @Schema(description = "Field ID") Long id,
        @Schema(description = "Field name", example = "Stade Central") String name,
        @Schema(description = "Sport type") SportType sportType,
        @Schema(description = "Indoor flag") boolean indoor,
        @Schema(description = "Price per hour") BigDecimal pricePerHour,
        @Schema(description = "Latitude") double latitude,
        @Schema(description = "Longitude") double longitude
) {
    /** Convenience factory to map from entity. */
    public static FieldResponse from(Field f) {
        return new FieldResponse(
                f.getId(),
                f.getName(),
                f.getSportType(),
                f.isIndoor(),
                f.getPricePerHour(),
                f.getLatitude(),
                f.getLongitude()
        );
    }
}
