package com.transport.reporting.modules.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private Long userId;
    private UUID uuid;
    private String username;
    private String name;
    private String email;
}
