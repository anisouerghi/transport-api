package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Vue d'ensemble statistiques — squelette extensible pour les futurs indicateurs.
 */
@Data
@Builder
public class StatisticsOverviewResponse {

    private long totalReports;
    private long totalSupports;
    private long totalUsers;
    private long totalPassengers;
    private long activePassengers;
    private long inactivePassengers;
}
