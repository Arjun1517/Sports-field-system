package com.sportsbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A bookable sports field.
 * When {@code indoor} is {@code false} the system will call Open-Meteo
 * before confirming reservations to check for adverse weather.
 */
@Entity
@Table(name = "fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SportType sportType;

    @Column(nullable = false)
    private boolean indoor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    /**
     * Geographical coordinates used for weather lookups on outdoor fields.
     * Default values point to Casablanca, Morocco — change per deployment.
     */
    @Column(nullable = false)
    @Builder.Default
    private double latitude = 33.5731;

    @Column(nullable = false)
    @Builder.Default
    private double longitude = -7.5898;

    public enum SportType {
        SOCCER, BASKETBALL, TENNIS
    }
}
