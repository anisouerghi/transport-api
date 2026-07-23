package com.transport.reporting.modules.voyageur.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VoyageurResponse {

    private Long id;
    private UUID uuid;
    private String nom;
    private String email;
    private String telephone;
}
