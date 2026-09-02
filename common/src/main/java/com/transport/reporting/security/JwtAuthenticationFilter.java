package com.transport.reporting.security;

import com.transport.reporting.entity.Passenger;
import com.transport.reporting.repository.PassengerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filtre JWT : agents ({@code typ=ADMIN}) et voyageurs ({@code typ=PASSENGER}).
 * Chaque JAR restreint les types acceptés via {@code app.security.accepted-token-types}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final PassengerRepository passengerRepository;
    private final Set<String> acceptedTokenTypes;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            PassengerRepository passengerRepository,
            @Value("${app.security.accepted-token-types:ADMIN,PASSENGER}") String acceptedTokenTypes) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passengerRepository = passengerRepository;
        this.acceptedTokenTypes = Arrays.stream(acceptedTokenTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/api/public/auth/google");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String typ = jwtService.extractTokenType(token);
                String normalized = typ == null
                        ? JwtService.TYPE_ADMIN
                        : typ.trim().toUpperCase(Locale.ROOT);
                if (!acceptedTokenTypes.contains(normalized)) {
                    SecurityContextHolder.clearContext();
                } else if (JwtService.TYPE_PASSENGER.equals(normalized)) {
                    authenticatePassenger(token, request);
                } else {
                    authenticateAdmin(token, request);
                }
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateAdmin(String token, HttpServletRequest request) {
        String username = jwtService.extractUsername(token);
        if (username == null) {
            return;
        }
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        if (jwtService.isTokenValid(token, principal)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    private void authenticatePassenger(String token, HttpServletRequest request) {
        Long passengerId = jwtService.extractPassengerId(token);
        if (passengerId == null) {
            return;
        }
        Passenger passenger = passengerRepository.findById(passengerId).orElse(null);
        if (passenger == null || !passenger.isActive()) {
            return;
        }
        PassengerPrincipal principal = new PassengerPrincipal(
                passenger.getPassengerId(),
                passenger.getEmail(),
                passenger.getName(),
                passenger.getPhoneNumber(),
                passenger.isActive());
        if (jwtService.isPassengerTokenValid(token, principal)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
