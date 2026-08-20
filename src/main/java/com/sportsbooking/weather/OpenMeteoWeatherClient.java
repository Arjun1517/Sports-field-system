package com.sportsbooking.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

/**
 * Fetches daily weather forecasts from the Open-Meteo free API.
 *
 * WHY RestClient instead of HttpExchange (declarative HTTP)?
 * ──────────────────────────────────────────────────────────
 * Spring Boot 3.2 ships HttpExchange support via @HttpExchange + HttpServiceProxyFactory,
 * but the declarative client requires a fixed URL template at compile time and does NOT
 * support dynamically composed query strings with varying runtime parameters (latitude,
 * longitude, date) easily through the annotation alone without extra interceptor wiring.
 * RestClient (introduced in Spring 6.1 / Boot 3.2) is the modern, fluent, synchronous
 * HTTP client that replaces RestTemplate and is the recommended choice for straightforward
 * REST calls where the URL and parameters are assembled at runtime.
 *
 * Open-Meteo endpoint used:
 *   GET https://api.open-meteo.com/v1/forecast
 *       ?latitude={lat}
 *       &longitude={lon}
 *       &daily=temperature_2m_max
 *       &start_date={date}
 *       &end_date={date}
 *       &timezone=auto
 */
@Component
public class OpenMeteoWeatherClient implements WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoWeatherClient.class);

    private final RestClient restClient;

    public OpenMeteoWeatherClient(
            @Value("${weather.api.base-url}") String baseUrl,
            RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public double getDailyMaxTemperature(double latitude, double longitude, LocalDate date) {
        String dateStr = date.toString(); // ISO-8601: yyyy-MM-dd

        log.info("Calling Open-Meteo: lat={}, lon={}, date={}", latitude, longitude, dateStr);

        try {
            WeatherResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude",    latitude)
                            .queryParam("longitude",   longitude)
                            .queryParam("daily",       "temperature_2m_max")
                            .queryParam("start_date",  dateStr)
                            .queryParam("end_date",    dateStr)
                            .queryParam("timezone",    "auto")
                            .build())
                    .retrieve()
                    .body(WeatherResponse.class);

            if (response == null || response.daily() == null) {
                throw new WeatherServiceException("Empty response from Open-Meteo API");
            }

            List<Double> temps = response.daily().temperature2mMax();
            if (temps == null || temps.isEmpty()) {
                throw new WeatherServiceException(
                        "No temperature data returned for date " + dateStr);
            }

            double temp = temps.get(0);
            log.info("Open-Meteo forecast for {}: max temp = {}°C", dateStr, temp);
            return temp;

        } catch (RestClientException e) {
            throw new WeatherServiceException(
                    "Failed to fetch weather data from Open-Meteo: " + e.getMessage(), e);
        }
    }
}
