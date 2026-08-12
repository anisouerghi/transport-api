package com.transport.reporting.security;

import com.transport.reporting.entity.Passenger;
import com.transport.reporting.repository.PassengerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT : agents ({@code typ=ADMIN}) et voyageurs ({@code typ=PASSENGER}).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final PassengerRepository passengerRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
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
                if (JwtService.TYPE_PASSENGER.equals(typ)) {
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
