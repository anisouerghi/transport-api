package com.transport.reporting.mapper;

import com.transport.reporting.dto.UserRequest;
import com.transport.reporting.dto.UserResponse;
import com.transport.reporting.entity.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AppUser toEntity(UserRequest request, String passwordHash) {
        return AppUser.builder()
                .username(request.getUsername())
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .build();
    }

    public void updateEntity(AppUser user, UserRequest request, String passwordHash) {
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHash);
    }

    public UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .uuid(user.getUuid())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
