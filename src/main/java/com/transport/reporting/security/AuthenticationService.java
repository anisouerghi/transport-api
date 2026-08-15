package com.transport.reporting.security;

import com.transport.reporting.dto.LoginRequest;
import com.transport.reporting.dto.LoginResponse;
import com.transport.reporting.dto.MenuItemResponse;
import com.transport.reporting.entity.AppMenu;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.AppMenuRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentification login : JWT + rôles / permissions / menus dynamiques.
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppMenuRepository appMenuRepository;
    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService, AppMenuRepository appMenuRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appMenuRepository = appMenuRepository;
    }


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return toLoginResponse(principal, jwtService.generateToken(principal));
        } catch (AuthenticationException ex) {
            throw new BusinessException("Invalid username or password");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse currentUser(UserPrincipal principal) {
        return toLoginResponse(principal, null);
    }

    public List<MenuItemResponse> resolveMenus(Set<String> permissions) {
        List<AppMenu> menus = appMenuRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return menus.stream()
                .filter(menu -> menu.getPermissionCode() == null
                        || menu.getPermissionCode().isBlank()
                        || permissions.contains(menu.getPermissionCode()))
                .map(menu -> MenuItemResponse.builder()
                        .code(menu.getCode())
                        .label(menu.getLabel())
                        .url(menu.getUrl())
                        .icon(menu.getIcon())
                        .permission(menu.getPermissionCode())
                        .build())
                .collect(Collectors.toList());
    }

    private LoginResponse toLoginResponse(UserPrincipal principal, String token) {
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(principal.getUserId())
                .username(principal.getUsername())
                .name(principal.getFullName())
                .email(principal.getEmail())
                .roles(List.copyOf(principal.getRoles()))
                .permissions(List.copyOf(principal.getPermissions()))
                .menus(resolveMenus(principal.getPermissions()))
                .build();
    }
}
