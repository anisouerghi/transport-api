package com.transport.reporting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PassengerRegisterRequest {

    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phoneNumber;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    /** GPS optionnel (navigateur) — jamais obligatoire. */
    private Double latitude;
    private Double longitude;
    private Double gpsAccuracy;

    /**
     * Token Cloudflare Turnstile (obligatoire si Turnstile est activé côté serveur).
     * Jamais persisté.
     */
    @Size(max = 2048)
    private String turnstileToken;
}
