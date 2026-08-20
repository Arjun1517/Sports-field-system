package com.sportsbooking.controller;

import com.sportsbooking.dto.LoginRequest;
import com.sportsbooking.dto.LoginResponse;
import com.sportsbooking.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Handles authentication — the only public endpoint in the API.
 *
 * POST /api/auth/login
 *   Body : { "username": "admin", "password": "admin123" }
 *   Returns a JWT Bearer token valid for app.jwt.expiration-ms milliseconds.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and obtain a JWT token")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns a JWT token for use in the Authorization header")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        log.info("Login attempt for username='{}'", request.username());

        // Spring Security authenticates credentials; throws AuthenticationException on failure.
        // The GlobalExceptionHandler maps that to HTTP 401.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        log.info("Login successful for username='{}'", request.username());

        return ResponseEntity.ok(new LoginResponse(token, jwtUtils.getExpirationMs()));
    }
}
