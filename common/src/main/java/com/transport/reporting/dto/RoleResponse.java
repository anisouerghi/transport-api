package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleResponse {

    private Long roleId;
    private String code;
    private String label;
    private String description;
    private boolean active;
    private List<PermissionResponse> permissions;
}
