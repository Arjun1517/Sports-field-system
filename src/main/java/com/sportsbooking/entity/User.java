package com.sportsbooking.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Application user — only ADMIN role exists in this system.
 * Passwords are stored as BCrypt hashes; never store plaintext.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt-hashed password — never expose this in responses. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    public enum Role {
        ADMIN
    }
}
