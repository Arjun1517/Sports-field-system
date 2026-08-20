package com.sportsbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Sports Field Booking System.
 *
 * Startup checklist:
 *   1. docker-compose up -d          (start MySQL)
 *   2. mvn spring-boot:run           (or run this class from the IDE)
 *   3. Open http://localhost:8080/swagger-ui.html
 *   4. POST /api/auth/login → copy token → click Authorize in Swagger UI
 */
@SpringBootApplication
public class SportsFieldBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsFieldBookingApplication.class, args);
    }
}
