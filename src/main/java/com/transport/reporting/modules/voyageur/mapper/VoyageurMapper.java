package com.transport.reporting.modules.voyageur.mapper;

import com.transport.reporting.modules.voyageur.dto.VoyageurRequest;
import com.transport.reporting.modules.voyageur.dto.VoyageurResponse;
import com.transport.reporting.modules.voyageur.entity.Voyageur;
import org.springframework.stereotype.Component;

@Component
public class VoyageurMapper {

    public Voyageur toEntity(VoyageurRequest request) {
        return Voyageur.builder()
                .nom(request.getNom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .build();
    }

    public VoyageurResponse toResponse(Voyageur voyageur) {
        return VoyageurResponse.builder()
                .id(voyageur.getId())
                .uuid(voyageur.getUuid())
                .nom(voyageur.getNom())
                .email(voyageur.getEmail())
                .telephone(voyageur.getTelephone())
                .build();
    }
}
