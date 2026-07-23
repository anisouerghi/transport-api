package com.transport.reporting.modules.voyageur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VoyageurRequest {

    @Size(max = 100)
    private String nom;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String telephone;
}
