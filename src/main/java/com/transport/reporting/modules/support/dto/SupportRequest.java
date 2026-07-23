package com.transport.reporting.modules.support.dto;

import com.transport.reporting.common.enums.TypeSupport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportRequest {

    @NotBlank
    @Size(max = 50)
    private String reference;

    @NotBlank
    @Size(max = 150)
    private String libelle;

    @NotNull
    private TypeSupport type;

    @Size(max = 500)
    private String qrCodeUrl;

    private Boolean actif;
}
