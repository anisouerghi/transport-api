package com.transport.reporting.service;

import com.transport.reporting.dto.PassengerRequest;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.mapper.PassengerMapper;
import com.transport.reporting.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final PassengerMapper passengerMapper;

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
                    .orElseGet(() -> passengerRepository.save(passengerMapper.toEntity(request)));
        }
        return passengerRepository.save(passengerMapper.toEntity(request));
    }
}
