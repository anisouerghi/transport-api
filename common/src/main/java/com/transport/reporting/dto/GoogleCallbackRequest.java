package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Échange d'un code OAuth éphémère (post-redirection Google) contre un JWT application.
 */
@Getter
@Setter
public class GoogleCallbackRequest {

    @NotBlank
    private String code;
}
