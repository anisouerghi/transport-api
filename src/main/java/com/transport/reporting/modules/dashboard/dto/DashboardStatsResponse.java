package com.transport.reporting.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {

    private long totalSignalements;
    private Map<String, Long> parStatut;
    private Map<String, Long> parType;
    private Double tempsMoyenTraitementHeures;
}
