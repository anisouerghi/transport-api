package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportAnalysisOption {
    private final Long id;
    private final String label;
}
