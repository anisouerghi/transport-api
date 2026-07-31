package com.transport.reporting.mapper;

import com.transport.reporting.dto.AuditLogResponse;
import com.transport.reporting.entity.AuditLog;
import org.springframework.stereotype.Component;

/**
 * Mapper Journal d'audit : Entity → DTO.
 */
@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .auditLogId(log.getAuditLogId())
                .actionDate(log.getActionDate())
                .userId(log.getUserId())
                .username(log.getUsername())
                .userFullName(log.getUserFullName())
                .ipAddress(log.getIpAddress())
                .actionType(log.getActionType())
                .module(log.getModule())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .description(log.getDescription())
                .userAgent(log.getUserAgent())
                .browser(log.getBrowser())
                .operatingSystem(log.getOperatingSystem())
                .result(log.getResult())
                .build();
    }
}
