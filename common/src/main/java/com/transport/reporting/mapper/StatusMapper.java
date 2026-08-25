package com.transport.reporting.mapper;

import com.transport.reporting.dto.StatusRequest;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.entity.Status;
import org.springframework.stereotype.Component;

/**
 * Mapper Status : conversion Entity <-> DTO.
 */
@Component
public class StatusMapper {

    public Status toEntity(StatusRequest request) {
        return Status.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .displayOrder(request.getDisplayOrder())
                .build();
    }

    public void updateEntity(Status status, StatusRequest request) {
        status.setCode(request.getCode());
        status.setLabel(request.getLabel());
        status.setDisplayOrder(request.getDisplayOrder());
    }

    public StatusResponse toResponse(Status status) {
        return StatusResponse.builder()
                .statusId(status.getStatusId())
                .code(status.getCode())
                .label(status.getLabel())
                .displayOrder(status.getDisplayOrder())
                .build();
    }
}