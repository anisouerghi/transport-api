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
}
