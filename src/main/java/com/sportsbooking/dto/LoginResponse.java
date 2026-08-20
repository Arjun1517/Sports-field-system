package com.sportsbooking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JWT token returned after a successful login.
 */
@Schema(description = "JWT authentication token response")
public record LoginResponse(

        @Schema(description = "Bearer JWT token to use in Authorization header")
        String token,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Expiry duration in milliseconds")
        long expiresIn
) {
    public LoginResponse(String token, long expiresIn) {
        this(token, "Bearer", expiresIn);
    }
}
