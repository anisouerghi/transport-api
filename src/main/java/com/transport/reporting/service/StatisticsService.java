package com.transport.reporting.service;

import com.transport.reporting.dto.StatisticsOverviewResponse;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Rapports & Statistiques (squelette extensible).
 * Les indicateurs métier avancés seront ajoutés progressivement.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final ReportRepository reportRepository;
    private final TransportSupportRepository transportSupportRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;

    /**
     * Vue d'ensemble minimale — base pour les futurs tableaux de bord.
     */
    public StatisticsOverviewResponse getOverview() {
        long totalPassengers = passengerRepository.count();
        long activePassengers = passengerRepository.countByActiveTrue();
        return StatisticsOverviewResponse.builder()
                .totalReports(reportRepository.count())
                .totalSupports(transportSupportRepository.count())
                .totalUsers(userRepository.count())
                .totalPassengers(totalPassengers)
                .activePassengers(activePassengers)
                .inactivePassengers(Math.max(0, totalPassengers - activePassengers))
                .build();
    }
}
