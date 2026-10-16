package com.transport.reporting.mapper;

import com.transport.reporting.dto.PermissionResponse;
import com.transport.reporting.dto.ReportTypeResponse;
import com.transport.reporting.dto.RoleResponse;
import com.transport.reporting.entity.Permission;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Role;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RoleMapper {

    public PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .permissionId(permission.getPermissionId())
                .code(permission.getCode())
                .label(permission.getLabel())
                .description(permission.getDescription())
                .moduleCode(permission.getModuleCode())
                .moduleLabel(permission.getModuleLabel())
                .actionCode(permission.getActionCode())
                .active(permission.isActive())
                .build();
    }

    public ReportTypeResponse toReportTypeResponse(ReportType type) {
        return ReportTypeResponse.builder()
                .reportTypeId(type.getReportTypeId())
                .code(type.getCode())
                .label(type.getLabel())
                .labelFr(type.getLabelFr())
                .labelAr(type.getLabelAr())
                .labelEn(type.getLabelEn())
                .description(type.getDescription())
                .priority(type.getPriority())
                .icon(type.getIcon())
                .active(type.isActive())
                .build();
    }

    public RoleResponse toResponse(Role role) {
        List<PermissionResponse> permissions = role.getPermissions() == null
                ? List.of()
                : role.getPermissions().stream().map(this::toPermissionResponse).collect(Collectors.toList());
        List<ReportTypeResponse> types = role.getReportTypes() == null
                ? List.of()
                : role.getReportTypes().stream()
                        .sorted(Comparator
                                .comparing((ReportType t) -> t.getPriority() == null ? Integer.MAX_VALUE : t.getPriority())
                                .thenComparing(t -> t.getLabel() == null ? "" : t.getLabel()))
                        .map(this::toReportTypeResponse)
                        .collect(Collectors.toList());
        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .code(role.getCode())
                .label(role.getLabel())
                .description(role.getDescription())
                .active(role.isActive())
                .permissions(permissions)
                .reportTypes(types)
                .build();
    }
}