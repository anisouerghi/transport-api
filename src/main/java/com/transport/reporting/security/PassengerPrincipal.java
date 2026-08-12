package com.transport.reporting.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal Spring Security pour un voyageur authentifié (JWT {@code typ=PASSENGER}).
 */
@Getter
public class PassengerPrincipal implements UserDetails {

    private final Long passengerId;
    private final String email;
    private final String name;
    private final String phoneNumber;
    private final boolean active;

    public PassengerPrincipal(Long passengerId, String email, String name, String phoneNumber, boolean active) {
        this.passengerId = passengerId;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.active = active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
