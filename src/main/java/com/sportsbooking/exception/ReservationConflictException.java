package com.sportsbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a new reservation overlaps an existing confirmed one,
 * or when weather conditions prevent an outdoor booking.
 * Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        super(message);
    }
}
