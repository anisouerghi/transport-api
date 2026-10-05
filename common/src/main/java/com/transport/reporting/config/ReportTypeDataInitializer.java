package com.transport.reporting.config;

import com.transport.reporting.entity.ReportType;
import com.transport.reporting.repository.ReportTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Assure le référentiel public des types de signalement (7 natures voyageur, FR/AR/EN).
 * Exécuté par public-api et admin-api (module common).
 * Idempotent : crée les codes manquants, complète les libellés i18n vides.
 */
@Configuration
public class ReportTypeDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReportTypeDataInitializer.class);

    /** Ordre d'affichage souhaité côté formulaire public. */
    private static final List<TypeSeed> SEEDS = List.of(
            new TypeSeed("COMPLAINT", "Réclamation", "شكوى", "Complaint",
                    "Réclamation voyageur", 2, "rate_review"),
            new TypeSeed("ASSAULT", "Agression", "اعتداء", "Assault",
                    "Signalement d'agression ou de violence", 7, "shield"),
            new TypeSeed("INCIDENT", "Incident", "حادث", "Incident",
                    "Incident technique ou sécurité", 3, "photo_camera"),
            new TypeSeed("SUGGESTION", "Suggestion", "اقتراح", "Suggestion",
                    "Suggestion d'amélioration", 4, "tips_and_updates"),
            new TypeSeed("THANKS", "Remerciement", "شكر", "Thank you",
                    "Remerciement", 5, "thumb_up"),
            new TypeSeed("OTHER", "Autre", "أخرى", "Other",
                    "Autre nature de signalement", 6, "contact_support"),
            new TypeSeed("URGENCE", "Urgence", "حالة طارئة أو خطر مباشر؟", "Emergency",
                    "Appel d'urgence 197 / 198", 1, "crisis_alert")
    );

    @Bean
    @Order(1)
    CommandLineRunner initReportTypes(ReportTypeRepository reportTypeRepository) {
        return args -> {
            int created = 0;
            int updated = 0;
            for (TypeSeed seed : SEEDS) {
                var existing = reportTypeRepository.findByCode(seed.code());
                if (existing.isEmpty()) {
                    reportTypeRepository.save(ReportType.builder()
                            .code(seed.code())
                            .label(seed.labelFr())
                            .labelFr(seed.labelFr())
                            .labelAr(seed.labelAr())
                            .labelEn(seed.labelEn())
                            .description(seed.description())
                            .priority(seed.priority())
                            .icon(seed.icon())
                            .active(true)
                            .build());
                    created++;
                } else {
                    ReportType entity = existing.get();
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
                    if (!entity.isActive()) {
                        entity.setActive(true);
                        changed = true;
                    }
                    if (entity.getPriority() == null) {
                        entity.setPriority(seed.priority());
                        changed = true;
                    }
                    if (!StringUtils.hasText(entity.getIcon())) {
                        entity.setIcon(seed.icon());
                        changed = true;
                    }
                    if (changed) {
                        if (!StringUtils.hasText(entity.getLabel())) {
                            entity.setLabel(entity.getLabelFr());
                        }
                        reportTypeRepository.save(entity);
                        updated++;
                    }
                }
            }
            log.info("Report types ensured ({} new, {} i18n patched)", created, updated);
        };
    }

    private record TypeSeed(String code, String labelFr, String labelAr, String labelEn, String description,
                             int priority, String icon) {
    }
}
