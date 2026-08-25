package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class RoleRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String label;

    @Size(max = 500)
    private String description;

    private Boolean active;

    private Set<Long> permissionIds = new HashSet<>();
}
