package com.transport.reporting.repository;

import com.transport.reporting.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    Optional<Permission> findByModuleCodeAndActionCode(String moduleCode, String actionCode);

    boolean existsByCode(String code);

    List<Permission> findByActiveTrueOrderByModuleCodeAscActionCodeAsc();

    List<Permission> findAllByOrderByModuleCodeAscActionCodeAsc();
}
