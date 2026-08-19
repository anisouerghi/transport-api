package com.transport.reporting.mapper;

import com.transport.reporting.dto.UserRequest;
import com.transport.reporting.dto.UserResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Role;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper Utilisateur : conversion Entity <-> DTO.
 */
@Component
public class UserMapper {

    public AppUser toEntity(UserRequest request, String passwordHash) {
        return AppUser.builder()
                .username(request.getUsername())
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .active(true)
                .createdDate(Instant.now())
                .build();
    }

    public void updateEntity(AppUser user, UserRequest request, String passwordHashOrNull) {
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        if (passwordHashOrNull != null) {
            user.setPasswordHash(passwordHashOrNull);
        }
    }

    public UserResponse toResponse(AppUser user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::getCode).sorted().collect(Collectors.toList());
        return UserResponse.builder()
                .userId(user.getUserId())
                .uuid(user.getUuid())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .active(user.isActive())
                .createdDate(user.getCreatedDate())
                .roles(roles)
                .build();
    }
}
