package com.sportsbooking.service;

import com.sportsbooking.dto.ReservationRequest;
import com.sportsbooking.dto.ReservationResponse;
import com.sportsbooking.entity.Field;
import com.sportsbooking.entity.Reservation;
import com.sportsbooking.entity.Reservation.Status;
import com.sportsbooking.exception.ReservationConflictException;
import com.sportsbooking.exception.ResourceNotFoundException;
import com.sportsbooking.repository.ReservationRepository;
import com.sportsbooking.weather.WeatherClient;
import com.sportsbooking.weather.WeatherServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final FieldService fieldService;
    private final WeatherClient weatherClient;

    @Value("${weather.temperature.min:10.0}")
    private double minTemperature;

    // ─── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public ReservationResponse create(ReservationRequest request) {

        // Basic hour sanity check (validation annotations cover most cases,
        // but an explicit guard here makes the service self-contained).
        if (request.startHour() >= request.endHour()) {
            throw new IllegalArgumentException(
                    "startHour (" + request.startHour() + ") must be less than endHour (" + request.endHour() + ")");
        }

        Field field = fieldService.getOrThrow(request.fieldId());

        log.info("Reservation attempt: field='{}' (id={}), client='{}', date={}, {}:00-{}:00",
                field.getName(), field.getId(),
                request.clientName(), request.date(),
                request.startHour(), request.endHour());

        // ── 1. Overlap check ──────────────────────────────────────────────────
        checkOverlap(field.getId(), request.date(), request.startHour(), request.endHour());

        // ── 2. Outdoor weather check ──────────────────────────────────────────
        if (!field.isIndoor()) {
            checkWeather(field, request.date());
        }

        // ── 3. Persist as CONFIRMED ───────────────────────────────────────────
        Reservation reservation = Reservation.builder()
                .field(field)
                .clientName(request.clientName())
                .clientPhone(request.clientPhone())
                .clientEmail(request.clientEmail())
                .date(request.date())
                .startHour(request.startHour())
                .endHour(request.endHour())
                .status(Status.CONFIRMED)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation confirmed: id={}, field='{}', date={}, {}:00-{}:00, client='{}'",
                saved.getId(), field.getName(), saved.getDate(),
                saved.getStartHour(), saved.getEndHour(), saved.getClientName());

        return ReservationResponse.from(saved);
    }

    // ─── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id) {
        return ReservationResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findByFieldAndDate(Long fieldId, LocalDate date) {
        // Ensure the field exists before querying reservations.
        fieldService.getOrThrow(fieldId);
        return reservationRepository.findByFieldIdAndDate(fieldId, date)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    // ─── Business rule helpers ─────────────────────────────────────────────────

    /**
     * Checks for time-slot overlap against existing CONFIRMED reservations.
     *
     * Overlap condition (Allen's interval overlap):
     *   existing.startHour < newEnd  AND  existing.endHour > newStart
     *
     * If any overlap exists the booking is rejected with HTTP 409.
     */
    void checkOverlap(Long fieldId, LocalDate date, int startHour, int endHour) {
        List<Reservation> conflicts = reservationRepository.findOverlapping(
                fieldId, date, startHour, endHour, Status.CONFIRMED);

        if (!conflicts.isEmpty()) {
            Reservation first = conflicts.get(0);
            String msg = String.format(
                    "Time slot %d:00-%d:00 on %s conflicts with an existing reservation (%d:00-%d:00).",
                    startHour, endHour, date,
                    first.getStartHour(), first.getEndHour());
            log.warn("Reservation REJECTED (overlap): {}", msg);
            throw new ReservationConflictException(msg);
        }
    }

    /**
     * Calls the weather API and rejects outdoor bookings when the forecast
     * maximum temperature on the reservation date is ≤ minTemperature (°C).
     *
     * If the weather service is unavailable the booking is also rejected
     * (fail-safe: no forecast → deny, do not silently allow).
     */
    void checkWeather(Field field, LocalDate date) {
        try {
            double maxTemp = weatherClient.getDailyMaxTemperature(
                    field.getLatitude(), field.getLongitude(), date);

            if (maxTemp <= minTemperature) {
                String msg = String.format(
                        "Outdoor field booking rejected: forecast temperature on %s is %.1f°C "
                        + "(minimum allowed: %.1f°C).",
                        date, maxTemp, minTemperature);
                log.warn("Reservation REJECTED (weather): {}", msg);
                throw new ReservationConflictException(msg);
            }
        } catch (WeatherServiceException e) {
            String msg = "Outdoor field booking rejected: unable to retrieve weather forecast — " + e.getMessage();
            log.error("Reservation REJECTED (weather service error): {}", e.getMessage());
            throw new ReservationConflictException(msg);
        }
    }

    // ─── Internal helper ───────────────────────────────────────────────────────

    private Reservation getOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found with id: " + id));
    }
}
