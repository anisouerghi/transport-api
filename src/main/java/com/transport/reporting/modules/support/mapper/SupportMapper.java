package com.transport.reporting.modules.support.mapper;

import com.transport.reporting.modules.support.dto.SupportRequest;
import com.transport.reporting.modules.support.dto.SupportResponse;
import com.transport.reporting.modules.support.entity.Support;
import org.springframework.stereotype.Component;

@Component
public class SupportMapper {

    public Support toEntity(SupportRequest request) {
        return Support.builder()
                .reference(request.getReference())
                .libelle(request.getLibelle())
                .type(request.getType())
                .qrCodeUrl(request.getQrCodeUrl())
                .actif(request.getActif() == null || request.getActif())
                .build();
    }

    public void updateEntity(Support support, SupportRequest request) {
        support.setReference(request.getReference());
        support.setLibelle(request.getLibelle());
        support.setType(request.getType());
        support.setQrCodeUrl(request.getQrCodeUrl());
        if (request.getActif() != null) {
            support.setActif(request.getActif());
        }
    }

    public SupportResponse toResponse(Support support) {
        return SupportResponse.builder()
                .id(support.getId())
                .uuid(support.getUuid())
                .reference(support.getReference())
                .libelle(support.getLibelle())
                .type(support.getType())
                .qrCodeUrl(support.getQrCodeUrl())
                .qrDateCreation(support.getQrDateCreation())
                .actif(support.isActif())
                .build();
    }
}
