package com.transport.reporting.modules.support.repository;

import com.transport.reporting.modules.support.entity.Support;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SupportRepository extends JpaRepository<Support, Long>, JpaSpecificationExecutor<Support> {

    Optional<Support> findByUuid(UUID uuid);

    Optional<Support> findByUuidAndActifTrue(UUID uuid);

    boolean existsByReference(String reference);
}
