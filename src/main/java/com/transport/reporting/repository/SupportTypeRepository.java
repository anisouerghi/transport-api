package com.transport.reporting.repository;

import com.transport.reporting.entity.SupportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository JPA des types de support.
 */
public interface SupportTypeRepository extends JpaRepository<SupportType, Long> {

    Optional<SupportType> findByCode(String code);

    boolean existsByCode(String code);
}
