package com.transport.reporting.repository;

import com.transport.reporting.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA des utilisateurs.
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByUuid(UUID uuid);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);
}
