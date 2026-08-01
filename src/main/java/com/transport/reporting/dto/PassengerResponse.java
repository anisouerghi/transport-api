package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PassengerResponse {

    private Long passengerId;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean emailVerified;
    private boolean active;
}
