package com.transport.reporting.dto;

import com.transport.reporting.common.enums.ReportAnalysisGrouping;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** Critères communs à l'aperçu, au PDF et à l'Excel. */
@Data
public class ReportAnalysisRequest {

    @Size(max = 160)
    private String title;

    @Size(max = 200)
    private String subtitle;

    private LocalDate from;
    private LocalDate to;
    private Long reportTypeId;
    private Long supportTypeId;
    private Long transportSupportId;
    private ReportAnalysisGrouping grouping;
}
