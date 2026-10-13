package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportAnalysisResponse {
    private final String title;
    private final String subtitle;
    private final String periodLabel;
    private final String reportTypeLabel;
    private final String supportTypeLabel;
    private final String supportLabel;
    private final String grouping;
    private final String groupingLabel;
    private final long totalReports;
    private final int groupCount;
    private final boolean detailTruncated;
    private final int detailLimit;
    private final String generatedAt;
    private final List<ReportAnalysisRow> rows;
}
