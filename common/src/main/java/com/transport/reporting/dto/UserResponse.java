package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
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
    private Instant createdDate;
    private List<String> roles;
}
