package com.sportsbooking.controller;

import com.sportsbooking.dto.ReservationRequest;
import com.sportsbooking.dto.ReservationResponse;
import com.sportsbooking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints for creating and viewing reservations.
 * All endpoints require a valid ADMIN JWT.
 *
 * POST /api/reservations                    — create reservation (overlap + weather check)
 * GET  /api/reservations?fieldId=&date=     — list reservations for a field on a date
 * GET  /api/reservations/{id}               — get single reservation
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Create and view field reservations (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(
        summary = "Create a reservation",
        description = "Validates time-slot overlap and (for outdoor fields) weather conditions. "
                    + "Returns HTTP 409 if either check fails.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reservation confirmed"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Field not found"),
        @ApiResponse(responseCode = "409", description = "Time overlap or weather rejection")
    })
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse saved = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @Operation(summary = "List reservations for a field on a date")
    public ResponseEntity<List<ReservationResponse>> getByFieldAndDate(
            @Parameter(description = "Field ID", required = true, example = "1")
            @RequestParam Long fieldId,

            @Parameter(description = "Date (ISO-8601: yyyy-MM-dd)", required = true, example = "2026-09-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(reservationService.findByFieldAndDate(fieldId, date));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }
}
