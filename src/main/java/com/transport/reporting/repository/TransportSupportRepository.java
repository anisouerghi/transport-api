package com.transport.reporting.repository;

import com.transport.reporting.entity.TransportSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransportSupportRepository extends JpaRepository<TransportSupport, Long> {

    Optional<TransportSupport> findByUuid(UUID uuid);

    boolean existsByReference(String reference);
}
