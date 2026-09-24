package com.transport.reporting.mapper;

import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.dto.StatusRequest;
import com.transport.reporting.dto.StatusResponse;
import com.transport.reporting.entity.Status;
import org.springframework.stereotype.Component;

/**
 * Mapper Status : conversion Entity &lt;-&gt; DTO.
 */
@Component
public class StatusMapper {

    public Status toEntity(StatusRequest request) {
        Status status = Status.builder()
                .code(request.getCode())
                .displayOrder(request.getDisplayOrder())
                .build();
        LocalizedLabels.applyFrench(status, request.getLabel());
        status.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        status.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        return status;
    }

    public void updateEntity(Status status, StatusRequest request) {
        status.setCode(request.getCode());
        LocalizedLabels.applyFrench(status, request.getLabel());
        status.setLabelAr(LocalizedLabels.trimToNull(request.getLabelAr()));
        status.setLabelEn(LocalizedLabels.trimToNull(request.getLabelEn()));
        status.setDisplayOrder(request.getDisplayOrder());
    }

    public StatusResponse toResponse(Status status) {
        String fr = LocalizedLabels.frOf(status.getLabelFr(), status.getLabel());
        return StatusResponse.builder()
                .statusId(status.getStatusId())
                .code(status.getCode())
                .label(LocalizedLabels.of(status))
                .labelFr(fr)
                .labelAr(status.getLabelAr())
                .labelEn(status.getLabelEn())
                .displayOrder(status.getDisplayOrder())
                .build();
    }
}
