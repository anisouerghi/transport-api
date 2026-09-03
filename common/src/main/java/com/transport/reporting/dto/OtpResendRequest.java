package com.transport.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpResendRequest {

    @NotBlank(message = "Identifiant de transaction OTP requis.")
    private String otpTransactionId;
}
