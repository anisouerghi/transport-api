package com.transport.reporting.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO reponse tableau de bord.
 */
@Data
@Builder
public class DashboardResponse {

    private long totalReports;
    private long totalSupports;
    private long totalUsers;
    private long totalPassengers;
}
