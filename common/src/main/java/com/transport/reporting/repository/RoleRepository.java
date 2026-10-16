package com.transport.reporting.repository;

import com.transport.reporting.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.roleId = :id")
    Optional<Role> findByIdWithPermissions(Long id);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();

    /** Second fetch pour éviter le produit cartésien permissions × types. */
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.reportTypes WHERE r.roleId IN :ids")
    List<Role> findWithReportTypesByIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.reportTypes WHERE r.roleId = :id")
    Optional<Role> findByIdWithReportTypes(@Param("id") Long id);

    /**
     * Union des types des rôles actifs de l'utilisateur (hors ADMIN géré côté service).
     */
    @Query("""
            SELECT DISTINCT t.reportTypeId
            FROM AppUser u
            JOIN u.roles r
            JOIN r.reportTypes t
            WHERE u.userId = :userId AND r.active = true
            """)
    List<Long> findAuthorizedReportTypeIdsByUserId(@Param("userId") Long userId);
}