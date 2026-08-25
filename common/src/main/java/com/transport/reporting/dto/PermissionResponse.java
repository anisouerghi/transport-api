package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {

    private Long permissionId;
    private String code;
    private String label;
    private String description;
    private String moduleCode;
    private String moduleLabel;
    private String actionCode;
    private boolean active;
}
