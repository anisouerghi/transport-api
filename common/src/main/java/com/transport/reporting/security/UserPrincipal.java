package com.transport.reporting.security;

import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Permission;
import com.transport.reporting.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link UserDetails} applicatif : permissions dynamiques comme authorities.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private final boolean active;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(AppUser user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getName();
        this.email = user.getEmail();
        this.active = user.isActive();

        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> permissionCodes = new LinkedHashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.isActive()) {
                    roleCodes.add(role.getCode());
                    if (role.getPermissions() != null) {
                        for (Permission permission : role.getPermissions()) {
                            if (permission.isActive()) {
                                permissionCodes.add(permission.getCode());
                            }
                        }
                    }
                }
            }
        }
        this.roles = roleCodes;
        this.permissions = permissionCodes;
        this.authorities = permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
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
