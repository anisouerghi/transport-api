package com.transport.reporting.repository;

import com.transport.reporting.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository JPA des voyageurs.
 */
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    Optional<Passenger> findByEmailIgnoreCase(String email);
}
