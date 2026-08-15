package com.transport.reporting.service;

import com.transport.reporting.dto.DashboardResponse;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service metier Tableau de bord / statistiques.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ReportRepository reportRepository;
    private final TransportSupportRepository transportSupportRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    public DashboardService(ReportRepository reportRepository, TransportSupportRepository transportSupportRepository, UserRepository userRepository, PassengerRepository passengerRepository) {
        this.reportRepository = reportRepository;
        this.transportSupportRepository = transportSupportRepository;
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
    }


    public DashboardResponse getDashboard() {
        return DashboardResponse.builder()
                .totalReports(reportRepository.count())
                .totalSupports(transportSupportRepository.count())
                .totalUsers(userRepository.count())
                .totalPassengers(passengerRepository.count())
                .build();
    }
}
