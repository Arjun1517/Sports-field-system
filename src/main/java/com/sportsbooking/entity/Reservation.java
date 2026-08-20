package com.sportsbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A time-slot reservation on a specific field.
 *
 * Overlap detection uses:
 *   existing.startHour < new.endHour AND existing.endHour > new.startHour
 *
 * Hours are stored as integers (0-23) representing the start of each hour.
 * e.g. startHour=9, endHour=11 means 09:00–11:00 (2 hours).
 */
@Entity
@Table(name = "reservations",
       indexes = {
           @Index(name = "idx_field_date", columnList = "field_id, date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The field being reserved. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(nullable = false, length = 100)
    private String clientName;

    @Column(nullable = false, length = 20)
    private String clientPhone;

    @Column(nullable = false, length = 100)
    private String clientEmail;

    @Column(nullable = false)
    private LocalDate date;

    /** Inclusive start hour (0-23). */
    @Column(nullable = false)
    private int startHour;

    /** Exclusive end hour (1-24, must be > startHour). */
    @Column(nullable = false)
    private int endHour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING, CONFIRMED, REJECTED
    }
}
