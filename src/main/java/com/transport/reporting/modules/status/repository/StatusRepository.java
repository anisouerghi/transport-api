package com.transport.reporting.modules.status.repository;

import com.transport.reporting.modules.status.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {

    Optional<Status> findByCode(String code);

    List<Status> findAllByOrderByDisplayOrderAsc();
}
