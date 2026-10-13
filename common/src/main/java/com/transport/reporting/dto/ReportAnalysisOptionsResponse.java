package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportAnalysisOptionsResponse {
    private final List<ReportAnalysisOption> reportTypes;
    private final List<ReportAnalysisOption> supportTypes;
}
