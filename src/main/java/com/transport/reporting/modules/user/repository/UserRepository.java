package com.transport.reporting.modules.user.repository;

import com.transport.reporting.modules.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByUuid(UUID uuid);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);
}
