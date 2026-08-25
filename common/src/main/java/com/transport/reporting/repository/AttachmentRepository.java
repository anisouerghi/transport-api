package com.transport.reporting.repository;

import com.transport.reporting.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository JPA des pieces jointes.
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByReport_ReportId(Long reportId);
}
