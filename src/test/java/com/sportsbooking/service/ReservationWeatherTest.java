package com.sportsbooking.service;

import com.sportsbooking.entity.Field;
import com.sportsbooking.entity.Field.SportType;
import com.sportsbooking.exception.ReservationConflictException;
import com.sportsbooking.repository.ReservationRepository;
import com.sportsbooking.weather.WeatherClient;
import com.sportsbooking.weather.WeatherServiceException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the weather-check business rule in ReservationService.
 *
 * Rule: For outdoor fields (indoor=false), if the forecast maximum temperature
 *       on the reservation date is ≤ 10°C, the booking must be REJECTED with
 *       HTTP 409 and a descriptive message.
 *       If the weather service is unreachable, reject conservatively (fail-safe).
 */
@ExtendWith(MockitoExtension.class)
class ReservationWeatherTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private FieldService fieldService;
    @Mock private WeatherClient weatherClient;

    @InjectMocks private ReservationService reservationService;

    private static final LocalDate DATE = LocalDate.of(2026, 9, 15);
    private static final double LAT  = 33.5731;
    private static final double LON  = -7.5898;
    private static final double MIN_TEMP = 10.0;

    private Field outdoorField;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservationService, "minTemperature", MIN_TEMP);

        outdoorField = Field.builder()
                .id(1L).name("Outdoor Soccer Field")
                .sportType(SportType.SOCCER)
                .indoor(false)
                .pricePerHour(BigDecimal.valueOf(100))
                .latitude(LAT).longitude(LON)
                .build();
    }

    // ── Temperature exactly at threshold ─────────────────────────────────────

    @Test
    @DisplayName("REJECT — forecast temp equals threshold (10.0°C): boundary check ≤")
    void reject_temperatureExactlyAtThreshold() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(10.0);

        assertThatThrownBy(() -> reservationService.checkWeather(outdoorField, DATE))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("10.0°C");
    }

    // ── Temperature below threshold ───────────────────────────────────────────

    @Test
    @DisplayName("REJECT — forecast temp is well below threshold (2.5°C)")
    void reject_temperatureBelowThreshold() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(2.5);

        assertThatThrownBy(() -> reservationService.checkWeather(outdoorField, DATE))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("2.5°C");
    }

    // ── Temperature just below threshold ─────────────────────────────────────

    @Test
    @DisplayName("REJECT — forecast temp just below threshold (9.9°C)")
    void reject_temperatureJustBelowThreshold() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(9.9);

        assertThatThrownBy(() -> reservationService.checkWeather(outdoorField, DATE))
                .isInstanceOf(ReservationConflictException.class);
    }

    // ── Temperature just above threshold ─────────────────────────────────────

    @Test
    @DisplayName("ALLOW — forecast temp just above threshold (10.1°C)")
    void allow_temperatureJustAboveThreshold() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(10.1);

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkWeather(outdoorField, DATE));
    }

    // ── Good weather ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("ALLOW — comfortable temperature (25.0°C)")
    void allow_goodWeather() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(25.0);

        assertThatNoException()
                .isThrownBy(() -> reservationService.checkWeather(outdoorField, DATE));
    }

    // ── Weather service unavailable (fail-safe) ───────────────────────────────

    @Test
    @DisplayName("REJECT — weather service throws WeatherServiceException (fail-safe)")
    void reject_weatherServiceUnavailable() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE))
                .thenThrow(new WeatherServiceException("Connection timeout"));

        assertThatThrownBy(() -> reservationService.checkWeather(outdoorField, DATE))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("unable to retrieve weather forecast");
    }

    // ── Rejection message includes useful context ─────────────────────────────

    @Test
    @DisplayName("REJECT — error message contains date and temperature for debuggability")
    void reject_errorMessageContainsDateAndTemp() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(5.0);

        assertThatThrownBy(() -> reservationService.checkWeather(outdoorField, DATE))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining(DATE.toString())
                .hasMessageContaining("5.0°C");
    }

    // ── Weather client is called with the field's exact coordinates ───────────

    @Test
    @DisplayName("Correct coordinates are passed to the weather client")
    void weatherClientCalledWithFieldCoordinates() {
        when(weatherClient.getDailyMaxTemperature(LAT, LON, DATE)).thenReturn(20.0);

        reservationService.checkWeather(outdoorField, DATE);

        verify(weatherClient).getDailyMaxTemperature(LAT, LON, DATE);
    }
}
