package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO reponse utilisateur.
 */
@Data
@Builder
public class UserResponse {

    private Long userId;
    private UUID uuid;
    private String username;
    private String name;
    private String email;
    private boolean active;
}
