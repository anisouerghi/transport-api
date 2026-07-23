package com.transport.reporting.mapper;

import com.transport.reporting.dto.PassengerRequest;
import com.transport.reporting.dto.PassengerResponse;
import com.transport.reporting.entity.Passenger;
import org.springframework.stereotype.Component;

@Component
public class PassengerMapper {

    public Passenger toEntity(PassengerRequest request) {
        return Passenger.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .emailVerified(false)
                .build();
    }

    public PassengerResponse toResponse(Passenger passenger) {
        return PassengerResponse.builder()
                .passengerId(passenger.getPassengerId())
                .name(passenger.getName())
                .email(passenger.getEmail())
                .phoneNumber(passenger.getPhoneNumber())
                .emailVerified(passenger.isEmailVerified())
                .build();
    }
}
