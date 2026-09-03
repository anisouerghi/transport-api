package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {

    @NotBlank(message = "Identifiant de transaction OTP requis.")
    private String otpTransactionId;

    @NotBlank(message = "Code OTP requis.")
    @Pattern(regexp = "^\\d{4,8}$", message = "Code OTP invalide.")
    private String otp;
}
