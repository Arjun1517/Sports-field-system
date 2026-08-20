package com.sportsbooking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI/Swagger UI to show a "Authorize" button
 * where users can paste their JWT and test protected endpoints directly.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sports Field Booking API",
                version = "1.0.0",
                description = "REST API for managing sports fields and reservations. " +
                              "Login at /api/auth/login to get a JWT, then click 'Authorize'."
        )
)
@SecurityScheme(
        name       = "bearerAuth",
        type       = SecuritySchemeType.HTTP,
        scheme     = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
