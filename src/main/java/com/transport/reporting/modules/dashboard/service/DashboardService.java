package com.transport.reporting.modules.dashboard.service;

import com.transport.reporting.modules.dashboard.dto.DashboardResponse;
import com.transport.reporting.modules.passenger.repository.PassengerRepository;
import com.transport.reporting.modules.report.repository.ReportRepository;
import com.transport.reporting.modules.support.repository.TransportSupportRepository;
import com.transport.reporting.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ReportRepository reportRepository;
    private final TransportSupportRepository transportSupportRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;

    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalReports(reportRepository.count())
                .totalSupports(transportSupportRepository.count())
                .totalUsers(userRepository.count())
                .totalPassengers(passengerRepository.count())
                .build();
    }
}
