package com.transport.reporting.mapper;

import com.transport.reporting.common.enums.PassengerLanguage;
import com.transport.reporting.dto.PassengerRequest;
import com.transport.reporting.dto.PassengerResponse;
import com.transport.reporting.entity.Passenger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class PassengerMapper {

    public Passenger toEntity(PassengerRequest request) {
        return Passenger.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .emailVerified(false)
                .active(true)
                .notifications(request.getNotifications() == null
                        ? null
                        : List.copyOf(request.getNotifications()))
                .language(request.getLanguage() == null
                        ? null
                        : PassengerLanguage.normalizeOrThrow(request.getLanguage()))
                .build();
    }

    /**
     * Applique une modification administrative. Les champs absents du corps de
     * requête sont laissés inchangés ; {@code notifications == []} vide les
     * préférences, {@code language == ""} efface la langue.
     */
    public void updateEntity(Passenger passenger, PassengerRequest request) {
        if (request.getName() != null) {
            passenger.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
        }
        if (request.getEmail() != null) {
            passenger.setEmail(
                    StringUtils.hasText(request.getEmail()) ? request.getEmail().trim().toLowerCase() : null);
        }
        if (request.getPhoneNumber() != null) {
            passenger.setPhoneNumber(
                    StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null);
        }
        if (request.getNotifications() != null) {
            passenger.setNotifications(List.copyOf(request.getNotifications()));
        }
        if (request.getLanguage() != null) {
            passenger.setLanguage(PassengerLanguage.normalizeOrThrow(request.getLanguage()));
        }
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
                .tracked(passenger.hasTrackedAccount())
                .notifications(passenger.getNotifications())
                .language(passenger.getLanguage())
                .build();
    }
}
