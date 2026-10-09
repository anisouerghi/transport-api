package com.transport.reporting.service;

import com.transport.reporting.common.i18n.LocalizedLabels;
import com.transport.reporting.dto.DashboardResponse;
import com.transport.reporting.dto.ReportTypeCountResponse;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.dto.ReportStatusCountResponse;
import com.transport.reporting.entity.Status;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Répartition des signalements par type, triée du plus fréquent au moins fréquent.
     *
     * <p>Seuls les types ayant au moins un signalement sont retournés (agrégation
     * de la table {@code report}). Le libellé suit l'{@code Accept-Language} de
     * l'appelant via {@link LocalizedLabels}.
     */
    public List<ReportTypeCountResponse> countReportsByType() {
        return reportRepository.countReportsGroupedByType().stream()
                .map(row -> {
                    ReportType type = (ReportType) row[0];
                    long count = ((Number) row[1]).longValue();
                    return ReportTypeCountResponse.builder()
                            .reportTypeId(type.getReportTypeId())
                            .code(type.getCode())
                            .label(LocalizedLabels.of(type))
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Répartition des signalements par statut, triée du plus fréquent au moins fréquent.
     *
     * <p>Seuls les statuts ayant au moins un signalement sont retournés (agrégation
     * de la table {@code report}). Le libellé suit l'{@code Accept-Language} de
     * l'appelant via {@link LocalizedLabels}.
     */
    public List<ReportStatusCountResponse> countReportsByStatus() {
        return reportRepository.countReportsGroupedByStatus().stream()
                .map(row -> {
                    Status status = (Status) row[0];
                    long count = ((Number) row[1]).longValue();
                    return ReportStatusCountResponse.builder()
                            .statusId(status.getStatusId())
                            .code(status.getCode())
                            .label(LocalizedLabels.of(status))
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
