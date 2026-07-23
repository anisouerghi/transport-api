package com.transport.reporting.modules.voyageur.repository;

import com.transport.reporting.modules.voyageur.entity.Voyageur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoyageurRepository extends JpaRepository<Voyageur, Long> {

    Optional<Voyageur> findByEmailIgnoreCase(String email);

    Optional<Voyageur> findByUuid(UUID uuid);
}
