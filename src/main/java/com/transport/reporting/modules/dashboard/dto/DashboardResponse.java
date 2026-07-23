package com.transport.reporting.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {

    private long totalReports;
    private long totalSupports;
    private long totalUsers;
    private long totalPassengers;
}
