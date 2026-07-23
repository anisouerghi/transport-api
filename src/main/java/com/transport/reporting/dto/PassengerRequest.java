package com.transport.reporting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PassengerRequest {

    @Size(max = 150)
    private String name;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phoneNumber;
}
