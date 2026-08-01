package com.transport.reporting.repository;

import com.transport.reporting.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger, Long>, JpaSpecificationExecutor<Passenger> {

    Optional<Passenger> findByEmailIgnoreCase(String email);

    long countByActiveTrue();
}
