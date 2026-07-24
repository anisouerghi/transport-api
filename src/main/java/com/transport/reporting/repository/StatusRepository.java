package com.transport.reporting.repository;

import com.transport.reporting.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA des statuts.
 */
public interface StatusRepository extends JpaRepository<Status, Long> {

    Optional<Status> findByCode(String code);

    List<Status> findAllByOrderByDisplayOrderAsc();
}
