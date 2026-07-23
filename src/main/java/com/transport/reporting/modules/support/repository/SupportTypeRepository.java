package com.transport.reporting.modules.support.repository;

import com.transport.reporting.modules.support.entity.SupportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportTypeRepository extends JpaRepository<SupportType, Long> {

    Optional<SupportType> findByCode(String code);

    boolean existsByCode(String code);
}
