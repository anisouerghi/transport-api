package com.transport.reporting.modules.report.repository;

import com.transport.reporting.modules.report.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByReport_ReportId(Long reportId);
}
