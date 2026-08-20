package com.sportsbooking.security;

import com.sportsbooking.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every request, extracts the JWT from the Authorization header,
 * validates it, and — if valid — populates the SecurityContext so that
 * downstream security checks know who the caller is.
 *
 * Filter chain position: runs before UsernamePasswordAuthenticationFilter.
 *
 * Request flow:
 *   1. Extract "Bearer <token>" from Authorization header.
 *   2. Parse username from token.
 *   3. Load UserDetails from the DB.
 *   4. Validate token signature + expiry.
 *   5. Set authentication in SecurityContextHolder.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No token — let the request continue; Security will reject it if
            // the endpoint requires authentication.
            filterChain.doFilter(request, response);
            return;
        }

        String token    = authHeader.substring(BEARER_PREFIX.length());
        String username = null;

        try {
            username = jwtUtils.extractUsername(token);
        } catch (Exception e) {
            log.warn("Could not extract username from JWT: {}", e.getMessage());
        }

        // Only set auth if we have a username and no auth is already in context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtils.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("JWT authenticated user: {}", username);
            }
        }

        filterChain.doFilter(request, response);
    }
}
