package com.transport.reporting.repository;

import com.transport.reporting.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles")
    List<AppUser> findAllWithRoles();

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<AppUser> findByUsernameWithRolesAndPermissions(String username);

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.userId = :id")
    Optional<AppUser> findByIdWithRolesAndPermissions(Long id);
}
