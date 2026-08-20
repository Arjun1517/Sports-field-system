package com.sportsbooking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted to POST /api/auth/login.
 */
@Schema(description = "Login credentials")
public record LoginRequest(

        @NotBlank(message = "Username is required")
        @Schema(description = "Admin username", example = "admin")
        String username,

        @NotBlank(message = "Password is required")
        @Schema(description = "Admin password", example = "admin123")
        String password
) {}
