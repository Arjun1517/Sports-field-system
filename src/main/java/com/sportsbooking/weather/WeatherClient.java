package com.sportsbooking.weather;

import java.time.LocalDate;

/**
 * Contract for fetching daily max temperature from a weather provider.
 * The implementation uses Open-Meteo's free forecast API.
 */
public interface WeatherClient {

    /**
     * Returns the forecast daily maximum temperature (°C) for the given
     * coordinates on the given date.
     *
     * @param latitude  decimal degrees
     * @param longitude decimal degrees
     * @param date      the date to look up
     * @return max temperature in °C for that day
     * @throws WeatherServiceException if the API call fails or returns no data
     */
    double getDailyMaxTemperature(double latitude, double longitude, LocalDate date);
}
