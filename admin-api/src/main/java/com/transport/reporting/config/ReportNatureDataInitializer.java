package com.transport.reporting.config;

import com.transport.reporting.entity.ReportNature;
import com.transport.reporting.repository.ReportNatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Initialise le référentiel des natures métier de signalement.
 */
@Configuration
public class ReportNatureDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReportNatureDataInitializer.class);

    private static final List<NatureSeed> SEEDS = List.of(
            new NatureSeed("AGRESSION", "Agression", "Signalements liés à une agression ou violence"),
            new NatureSeed("PROPRETE", "Propreté", "Signalements liés à la propreté des véhicules et stations"),
            new NatureSeed("SECURITE", "Sécurité", "Signalements liés à la sécurité des voyageurs et des biens"),
            new NatureSeed("MAINTENANCE", "Maintenance", "Signalements liés à la maintenance ou aux pannes"),
            new NatureSeed("INFORMATION", "Information", "Demandes ou manques d'information"),
            new NatureSeed("COMPORTEMENT", "Comportement", "Signalements liés au comportement (voyageurs ou agents)"),
            new NatureSeed("RETARD", "Retard", "Signalements liés aux retards et perturbations"),
            new NatureSeed("ACCESSIBILITE", "Accessibilité", "Signalements liés à l'accessibilité"),
            new NatureSeed("AUTRE", "Autre", "Autres natures non listées")
    );

    @Bean
    @Order(2)
    CommandLineRunner initReportNatures(ReportNatureRepository reportNatureRepository) {
        return args -> {
            int created = 0;
            for (NatureSeed seed : SEEDS) {
                if (reportNatureRepository.findByCode(seed.code()).isEmpty()) {
                    reportNatureRepository.save(ReportNature.builder()
                            .code(seed.code())
                            .label(seed.label())
                            .description(seed.description())
                            .active(true)
                            .build());
                    created++;
                }
            }
            log.info("Report natures ensured ({} new)", created);
        };
    }

    private record NatureSeed(String code, String label, String description) {
    }
}
