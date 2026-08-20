package com.sportsbooking.service;

import com.sportsbooking.entity.Field;
import com.sportsbooking.entity.Field.SportType;
import com.sportsbooking.entity.Reservation;
import com.sportsbooking.entity.Reservation.Status;
import com.sportsbooking.exception.ReservationConflictException;
import com.sportsbooking.repository.ReservationRepository;
import com.sportsbooking.weather.WeatherClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the time-slot overlap detection logic in ReservationService.
 *
 * The overlap condition under test:
 *   existing.startHour < newEnd  AND  existing.endHour > newStart
 *
 * Visual legend:  [  ] = existing slot,  {  } = new slot
 *
 *  OVERLAPPING cases:
 *   1. New slot starts before existing ends and ends inside it:   [  { ]  }
 *   2. New slot fully contains existing slot:                      {  [  ]  }
 *   3. New slot starts inside existing slot:                       [  {  ]  }
 *   4. Exact same slot:                                            [ { ] }
 *
 *  NON-OVERLAPPING cases:
 *   5. New slot is entirely before existing:                       {  }  [  ]
 *   6. New slot is entirely after existing:                        [  ]  {  }
 *   7. New end == existing start (adjacent, touching):             {  }[  ]
 *   8. New start == existing end (adjacent, touching):             [  ]{  }
 */
@ExtendWith(MockitoExtension.class)
class ReservationOverlapTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private FieldService fieldService;
    @Mock private WeatherClient weatherClient;

    @InjectMocks private ReservationService reservationService;

    private static final Long FIELD_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 9, 15);

    @BeforeEach
    void setUp() {
        // Inject the @Value field manually since we're not loading Spring context
        ReflectionTestUtils.setField(reservationService, "minTemperature", 10.0);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Reservation existingReservation(int start, int end) {
        Field field = Field.builder()
                .id(FIELD_ID).name("Test Field")
                .sportType(SportType.SOCCER)
                .indoor(true)
                .pricePerHour(BigDecimal.TEN)
                .build();

        return Reservation.builder()
                .id(99L).field(field)
                .clientName("Existing Client")
                .clientPhone("0600000000").clientEmail("existing@test.com")
                .date(DATE).startHour(start).endHour(end)
                .status(Status.CONFIRMED)
                .build();
    }

    private void mockOverlapQuery(int newStart, int newEnd, List<Reservation> results) {
        when(reservationRepository.findOverlapping(FIELD_ID, DATE, newStart, newEnd, Status.CONFIRMED))
                .thenReturn(results);
    }

    // ── Case 1: New slot partially overlaps the end of existing ──────────────

    @Test
    @DisplayName("REJECT — new slot starts before existing ends and ends inside it: [ { ] }")
    void overlap_newStartsInsideExisting() {
        // existing: 9-11,  new: 10-13  → overlap at 10-11
        mockOverlapQuery(10, 13, List.of(existingReservation(9, 11)));

        assertThatThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 10, 13))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("conflicts");
    }

    // ── Case 2: New slot fully contains existing ──────────────────────────────

    @Test
    @DisplayName("REJECT — new slot fully wraps the existing slot: { [ ] }")
    void overlap_newFullyContainsExisting() {
        // existing: 10-11,  new: 9-12  → new wraps existing
        mockOverlapQuery(9, 12, List.of(existingReservation(10, 11)));

        assertThatThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 9, 12))
                .isInstanceOf(ReservationConflictException.class);
    }

    // ── Case 3: New slot starts inside existing ───────────────────────────────

    @Test
    @DisplayName("REJECT — new slot starts inside existing and ends after it: [ { ] }")
    void overlap_newEndsAfterExisting() {
        // existing: 9-12,  new: 10-14  → overlap at 10-12
        mockOverlapQuery(10, 14, List.of(existingReservation(9, 12)));

        assertThatThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 10, 14))
                .isInstanceOf(ReservationConflictException.class);
    }

    // ── Case 4: Exact same slot ────────────────────────────────────────────────

    @Test
    @DisplayName("REJECT — new slot is identical to an existing one")
    void overlap_exactSameSlot() {
        mockOverlapQuery(9, 11, List.of(existingReservation(9, 11)));

        assertThatThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 9, 11))
                .isInstanceOf(ReservationConflictException.class);
    }

    // ── Case 5: New slot is entirely before existing ──────────────────────────

    @Test
    @DisplayName("ALLOW — new slot ends before existing starts: { }  [ ]")
    void noOverlap_newBeforeExisting() {
        // existing: 12-14,  new: 9-11  → no overlap
        mockOverlapQuery(9, 11, List.of());

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 9, 11));
    }

    // ── Case 6: New slot is entirely after existing ───────────────────────────

    @Test
    @DisplayName("ALLOW — new slot starts after existing ends: [ ]  { }")
    void noOverlap_newAfterExisting() {
        // existing: 9-11,  new: 14-16  → no overlap
        mockOverlapQuery(14, 16, List.of());

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 14, 16));
    }

    // ── Case 7: Adjacent — new end exactly equals existing start ─────────────

    @Test
    @DisplayName("ALLOW — new ends exactly when existing starts (touching, not overlapping): { }[ ]")
    void noOverlap_newEndsAtExistingStart() {
        // existing: 11-13,  new: 9-11  → touching at 11 but NOT overlapping
        mockOverlapQuery(9, 11, List.of());

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 9, 11));
    }

    // ── Case 8: Adjacent — new start exactly equals existing end ─────────────

    @Test
    @DisplayName("ALLOW — new starts exactly when existing ends (touching, not overlapping): [ ]{ }")
    void noOverlap_newStartsAtExistingEnd() {
        // existing: 9-11,  new: 11-13  → touching at 11 but NOT overlapping
        mockOverlapQuery(11, 13, List.of());

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 11, 13));
    }

    // ── No conflicts → no exception ───────────────────────────────────────────

    @Test
    @DisplayName("ALLOW — empty conflict list means no existing reservations block the slot")
    void noOverlap_emptyConflictList() {
        mockOverlapQuery(8, 10, List.of());

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkOverlap(FIELD_ID, DATE, 8, 10));
    }
}
