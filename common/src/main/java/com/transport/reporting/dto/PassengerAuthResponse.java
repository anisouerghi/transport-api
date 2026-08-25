package com.transport.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerAuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresInMs;
    private Long passengerId;
    private String name;
    private String email;
    private String phoneNumber;
}
