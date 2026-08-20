package com.sportsbooking.repository;

import com.sportsbooking.entity.Reservation;
import com.sportsbooking.entity.Reservation.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Returns all CONFIRMED reservations for a field on a given date
     * that overlap with the [startHour, endHour) window.
     *
     * Overlap condition: existing.startHour < newEnd AND existing.endHour > newStart
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.field.id = :fieldId
              AND r.date       = :date
              AND r.status     = :status
              AND r.startHour  < :endHour
              AND r.endHour    > :startHour
            """)
    List<Reservation> findOverlapping(
            @Param("fieldId")   Long fieldId,
            @Param("date")      LocalDate date,
            @Param("startHour") int startHour,
            @Param("endHour")   int endHour,
            @Param("status")    Status status
    );

    /** All reservations for a field on a specific date (for the GET query endpoint). */
    List<Reservation> findByFieldIdAndDate(Long fieldId, LocalDate date);
}
