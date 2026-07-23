package com.transport.reporting.modules.signalement.repository;

import com.transport.reporting.common.enums.StatutSignalement;
import com.transport.reporting.modules.signalement.entity.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SignalementRepository extends JpaRepository<Signalement, Long>, JpaSpecificationExecutor<Signalement> {

    Optional<Signalement> findByReference(String reference);

    boolean existsByReference(String reference);

    long countByStatut(StatutSignalement statut);
}
