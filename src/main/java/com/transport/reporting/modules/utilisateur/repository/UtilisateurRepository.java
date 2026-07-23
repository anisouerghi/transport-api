package com.transport.reporting.modules.utilisateur.repository;

import com.transport.reporting.modules.utilisateur.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmailIgnoreCase(String email);
}
