package com.transport.reporting.modules.report.repository;

import com.transport.reporting.modules.report.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByReport_ReportIdOrderByReplyDateAsc(Long reportId);
}
