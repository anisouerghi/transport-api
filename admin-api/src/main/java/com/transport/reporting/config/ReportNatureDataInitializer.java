package com.transport.reporting.config;

import com.transport.reporting.entity.ReportNature;
import com.transport.reporting.repository.ReportNatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Initialise le référentiel des natures métier de signalement (FR/AR/EN).
 */
@Configuration
public class ReportNatureDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReportNatureDataInitializer.class);

    private static final List<NatureSeed> SEEDS = List.of(
            new NatureSeed("AGRESSION", "Agression", "اعتداء", "Assault",
                    "Signalements liés à une agression ou violence"),
            new NatureSeed("PROPRETE", "Propreté", "نظافة", "Cleanliness",
                    "Signalements liés à la propreté des véhicules et stations"),
            new NatureSeed("SECURITE", "Sécurité", "أمن", "Security",
                    "Signalements liés à la sécurité des voyageurs et des biens"),
            new NatureSeed("MAINTENANCE", "Maintenance", "صيانة", "Maintenance",
                    "Signalements liés à la maintenance ou aux pannes"),
            new NatureSeed("INFORMATION", "Information", "معلومة", "Information",
                    "Demandes ou manques d'information"),
            new NatureSeed("COMPORTEMENT", "Comportement", "سلوك", "Behavior",
                    "Signalements liés au comportement (voyageurs ou agents)"),
            new NatureSeed("RETARD", "Retard", "تأخير", "Delay",
                    "Signalements liés aux retards et perturbations"),
            new NatureSeed("ACCESSIBILITE", "Accessibilité", "إمكانية الوصول", "Accessibility",
                    "Signalements liés à l'accessibilité"),
            new NatureSeed("AUTRE", "Autre", "أخرى", "Other",
                    "Autres natures non listées")
    );

    @Bean
    @Order(2)
    CommandLineRunner initReportNatures(ReportNatureRepository reportNatureRepository) {
        return args -> {
            int created = 0;
            int updated = 0;
            for (NatureSeed seed : SEEDS) {
                var existing = reportNatureRepository.findByCode(seed.code());
                if (existing.isEmpty()) {
                    reportNatureRepository.save(ReportNature.builder()
                            .code(seed.code())
                            .label(seed.labelFr())
                            .labelFr(seed.labelFr())
                            .labelAr(seed.labelAr())
                            .labelEn(seed.labelEn())
                            .description(seed.description())
                            .active(true)
                            .build());
                    created++;
                } else {
                    ReportNature entity = existing.get();
                    boolean changed = false;
                    if (!StringUtils.hasText(entity.getLabelFr())) {
                        entity.setLabelFr(StringUtils.hasText(entity.getLabel()) ? entity.getLabel() : seed.labelFr());
                        changed = true;
                    }
                    if (!StringUtils.hasText(entity.getLabelAr())) {
                        entity.setLabelAr(seed.labelAr());
                        changed = true;
                    }
                    if (!StringUtils.hasText(entity.getLabelEn())) {
                        entity.setLabelEn(seed.labelEn());
                        changed = true;
                    }
                    if (changed) {
                        if (!StringUtils.hasText(entity.getLabel())) {
                            entity.setLabel(entity.getLabelFr());
                        }
                        reportNatureRepository.save(entity);
                        updated++;
                    }
                }
            }
            log.info("Report natures ensured ({} new, {} i18n patched)", created, updated);
        };
    }

    private record NatureSeed(String code, String labelFr, String labelAr, String labelEn, String description) {
    }
}
