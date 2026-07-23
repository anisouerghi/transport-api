package com.transport.reporting.modules.passenger.service;

import com.transport.reporting.modules.passenger.dto.PassengerRequest;
import com.transport.reporting.modules.passenger.dto.PassengerResponse;
import com.transport.reporting.modules.passenger.entity.Passenger;
import com.transport.reporting.modules.passenger.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class PassengerService {

    private final PassengerRepository passengerRepository;

    public Passenger findOrCreate(PassengerRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return passengerRepository.findByEmailIgnoreCase(request.getEmail())
                    .map(existing -> {
                        if (request.getName() != null) {
                            existing.setName(request.getName());
                        }
                        if (request.getPhoneNumber() != null) {
                            existing.setPhoneNumber(request.getPhoneNumber());
                        }
                        return passengerRepository.save(existing);
                    })
                    .orElseGet(() -> passengerRepository.save(toEntity(request)));
        }
        return passengerRepository.save(toEntity(request));
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

    private Passenger toEntity(PassengerRequest request) {
        return Passenger.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .emailVerified(false)
                .build();
    }
}
