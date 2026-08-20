package com.sportsbooking.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Subset of the Open-Meteo /v1/forecast JSON response that we care about.
 *
 * Full schema: https://open-meteo.com/en/docs
 *
 * Example relevant portion:
 * {
 *   "daily": {
 *     "time":            ["2026-09-15"],
 *     "temperature_2m_max": [22.4]
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherResponse(Daily daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(

            @JsonProperty("time")
            List<String> time,

            @JsonProperty("temperature_2m_max")
            List<Double> temperature2mMax
    ) {}
}
