package com.sportsbooking.weather;

/**
 * Thrown when the weather API cannot be reached or returns unusable data.
 * The caller (ReservationService) catches this and rejects the booking
 * conservatively (fail-safe: no forecast → deny outdoor booking).
 */
public class WeatherServiceException extends RuntimeException {

    public WeatherServiceException(String message) {
        super(message);
    }

    public WeatherServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
