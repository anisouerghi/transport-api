package com.transport.reporting.mapper;

import com.transport.reporting.dto.SupportTypeRequest;
import com.transport.reporting.dto.SupportTypeResponse;
import com.transport.reporting.entity.SupportType;
import org.springframework.stereotype.Component;

/**
 * Mapper SupportType : conversion Entity &lt;-&gt; DTO.
 * Pattern identique a UserMapper (manuel, pas MapStruct).
 */
@Component
public class SupportTypeMapper {

    /** Request -> nouvelle entite (creation). */
    public SupportType toEntity(SupportTypeRequest request) {
        return SupportType.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .build();
    }

    /** Applique le Request sur une entite existante (modification). */
    public void updateEntity(SupportType entity, SupportTypeRequest request) {
        entity.setCode(request.getCode());
        entity.setLabel(request.getLabel());
    }

    /** Entite -> Response (envoye au frontend). */
    public SupportTypeResponse toResponse(SupportType entity) {
        return SupportTypeResponse.builder()
                .supportTypeId(entity.getSupportTypeId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .build();
    }
}
