package com.transport.reporting.modules.dashboard.service;

import com.transport.reporting.common.enums.StatutSignalement;
import com.transport.reporting.common.enums.TypeSignalement;
import com.transport.reporting.modules.dashboard.dto.DashboardStatsResponse;
import com.transport.reporting.modules.signalement.entity.Signalement;
import com.transport.reporting.modules.signalement.repository.SignalementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final SignalementRepository signalementRepository;

    @Override
    public DashboardStatsResponse getStats() {
        List<Signalement> all = signalementRepository.findAll();

        Map<String, Long> parStatut = Arrays.stream(StatutSignalement.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        statut -> all.stream().filter(s -> s.getStatut() == statut).count(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, Long> parType = Arrays.stream(TypeSignalement.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        type -> all.stream().filter(s -> s.getType() == type).count(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return DashboardStatsResponse.builder()
                .totalSignalements(all.size())
                .parStatut(parStatut)
                .parType(parType)
                .tempsMoyenTraitementHeures(null)
                .build();
    }
}
