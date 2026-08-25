package com.transport.reporting.mapper;

import com.transport.reporting.dto.PassengerRequest;
import com.transport.reporting.dto.PassengerResponse;
import com.transport.reporting.entity.Passenger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PassengerMapper {

    public Passenger toEntity(PassengerRequest request) {
        return Passenger.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .emailVerified(false)
                .active(true)
                .build();
    }

    public PassengerResponse toResponse(Passenger passenger) {
        boolean anonymous = !StringUtils.hasText(passenger.getName())
                && !StringUtils.hasText(passenger.getEmail())
                && !StringUtils.hasText(passenger.getPhoneNumber());
        return PassengerResponse.builder()
                .passengerId(passenger.getPassengerId())
                .name(passenger.getName())
                .email(passenger.getEmail())
                .phoneNumber(passenger.getPhoneNumber())
                .emailVerified(passenger.isEmailVerified())
                .active(passenger.isActive())
                .anonymous(anonymous)
                .build();
    }
}
