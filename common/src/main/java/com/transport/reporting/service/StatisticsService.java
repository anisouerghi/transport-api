package com.transport.reporting.service;

import com.transport.reporting.dto.StatisticsOverviewResponse;
import com.transport.reporting.entity.Report;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Rapports & Statistiques (squelette extensible).
 * Les indicateurs métier avancés seront ajoutés progressivement.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final ReportRepository reportRepository;
    private final TransportSupportRepository transportSupportRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final ReportTypeAccessService reportTypeAccessService;

    public StatisticsService(
            ReportRepository reportRepository,
            TransportSupportRepository transportSupportRepository,
            UserRepository userRepository,
            PassengerRepository passengerRepository,
            ReportTypeAccessService reportTypeAccessService) {
        this.reportRepository = reportRepository;
        this.transportSupportRepository = transportSupportRepository;
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
        this.reportTypeAccessService = reportTypeAccessService;
    }

    /**
     * Vue d'ensemble minimale — le total des signalements respecte le périmètre de types.
     */
    public StatisticsOverviewResponse getOverview() {
        long totalPassengers = passengerRepository.count();
        long activePassengers = passengerRepository.countByActiveTrue();
        long totalReports = reportRepository.count(reportTypeAccessService.<Report>asSpecification());
        return StatisticsOverviewResponse.builder()
                .totalReports(totalReports)
                .totalSupports(transportSupportRepository.count())
                .totalUsers(userRepository.count())
                .totalPassengers(totalPassengers)
                .activePassengers(activePassengers)
                .inactivePassengers(Math.max(0, totalPassengers - activePassengers))
                .build();
    }
}
