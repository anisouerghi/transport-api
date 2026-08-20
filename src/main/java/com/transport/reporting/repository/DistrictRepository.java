package com.transport.reporting.repository;

import com.transport.reporting.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository JPA des districts.
 */
public interface DistrictRepository extends JpaRepository<District, Long>, JpaSpecificationExecutor<District> {

    boolean existsByCodeDistrict(String codeDistrict);
}
