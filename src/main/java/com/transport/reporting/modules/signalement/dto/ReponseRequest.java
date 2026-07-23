package com.transport.reporting.modules.signalement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReponseRequest {

    @NotBlank
    @Size(max = 5000)
    private String reponse;
}
