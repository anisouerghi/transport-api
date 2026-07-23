package com.transport.reporting.modules.support.repository;

import com.transport.reporting.modules.support.entity.TransportSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransportSupportRepository extends JpaRepository<TransportSupport, Long> {

    Optional<TransportSupport> findByUuid(UUID uuid);

    boolean existsByReference(String reference);
}
