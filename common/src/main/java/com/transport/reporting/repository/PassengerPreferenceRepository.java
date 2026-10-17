package com.transport.reporting.repository;

import com.transport.reporting.entity.PassengerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassengerPreferenceRepository extends JpaRepository<PassengerPreference, Long> {

    Optional<PassengerPreference> findByPassenger_PassengerId(Long passengerId);

    boolean existsByPassenger_PassengerId(Long passengerId);
}
