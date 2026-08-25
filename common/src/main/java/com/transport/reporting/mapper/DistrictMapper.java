package com.transport.reporting.mapper;

import com.transport.reporting.dto.DistrictRequest;
import com.transport.reporting.dto.DistrictResponse;
import com.transport.reporting.entity.District;
import org.springframework.stereotype.Component;

/**
 * Mapper District : conversion Entity <-> DTO.
 */
@Component
public class DistrictMapper {

    /** Request -> nouvelle entite (creation). */
    public District toEntity(DistrictRequest request) {
        return District.builder()
                .codeDistrict(request.getCodeDistrict())
                .libelleDistrict(request.getLibelleDistrict())
                .etat(request.getEtat() != null ? request.getEtat() : 1)
                .build();
    }

    /** Applique le Request sur une entite existante (modification). */
    public void updateEntity(District entity, DistrictRequest request) {
        entity.setCodeDistrict(request.getCodeDistrict());
        entity.setLibelleDistrict(request.getLibelleDistrict());
        if (request.getEtat() != null) {
            entity.setEtat(request.getEtat());
        }
    }

    /** Entite -> Response (envoye au frontend). */
    public DistrictResponse toResponse(District entity) {
        return DistrictResponse.builder()
                .districtId(entity.getDistrictId())
                .codeDistrict(entity.getCodeDistrict())
                .libelleDistrict(entity.getLibelleDistrict())
                .etat(entity.getEtat())
                .build();
    }
}
