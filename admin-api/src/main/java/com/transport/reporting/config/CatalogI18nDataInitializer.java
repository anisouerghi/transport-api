package com.transport.reporting.config;

import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.SupportTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Complète label_fr / label_ar / label_en sur les catalogues connus (idempotent).
 */
@Configuration
public class CatalogI18nDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(CatalogI18nDataInitializer.class);

    private static final Map<String, String[]> SUPPORT_TYPES = Map.of(
            "BUS", new String[]{"Bus", "حافلة", "Bus"},
            "METRO", new String[]{"Métro", "مترو", "Metro"},
            "TRAIN", new String[]{"Train", "قطار", "Train"},
            "STATION", new String[]{"Station", "محطة", "Station"}
    );

    private static final Map<String, String[]> REPORT_TYPES = Map.of(
            "INCIDENT", new String[]{"Incident", "حادث", "Incident"},
            "COMPLAINT", new String[]{"Réclamation", "شكوى", "Complaint"},
            "SUGGESTION", new String[]{"Suggestion", "اقتراح", "Suggestion"}
    );

    private static final Map<String, String[]> STATUSES = Map.of(
            "NEW", new String[]{"Nouveau", "جديد", "New"},
            "IN_PROGRESS", new String[]{"En cours", "قيد المعالجة", "In progress"},
            "RESOLVED", new String[]{"Résolu", "محلول", "Resolved"},
            "CLOSED", new String[]{"Clôturé", "مغلق", "Closed"}
    );

    @Bean
    @Order(3)
    CommandLineRunner patchCatalogI18n(
            SupportTypeRepository supportTypeRepository,
            ReportTypeRepository reportTypeRepository,
            StatusRepository statusRepository) {
        return args -> {
            int n = 0;
            for (SupportType entity : supportTypeRepository.findAll()) {
                String[] t = SUPPORT_TYPES.get(entity.getCode());
                if (t != null && patch(entity, t)) {
                    supportTypeRepository.save(entity);
                    n++;
                } else if (ensureFrMirror(entity)) {
                    supportTypeRepository.save(entity);
                    n++;
                }
            }
            for (ReportType entity : reportTypeRepository.findAll()) {
                String[] t = REPORT_TYPES.get(entity.getCode());
                if (t != null && patch(entity, t)) {
                    reportTypeRepository.save(entity);
                    n++;
                } else if (ensureFrMirror(entity)) {
                    reportTypeRepository.save(entity);
                    n++;
                }
            }
            for (Status entity : statusRepository.findAll()) {
                String[] t = STATUSES.get(entity.getCode());
                if (t != null && patch(entity, t)) {
                    statusRepository.save(entity);
                    n++;
                } else if (ensureFrMirror(entity)) {
                    statusRepository.save(entity);
                    n++;
                }
            }
            log.info("Catalog i18n labels patched ({} rows)", n);
        };
    }

    private static boolean patch(SupportType entity, String[] frArEn) {
        boolean changed = false;
        if (!StringUtils.hasText(entity.getLabelFr())) {
            entity.setLabelFr(frArEn[0]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabelAr())) {
            entity.setLabelAr(frArEn[1]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabelEn())) {
            entity.setLabelEn(frArEn[2]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabel()) && StringUtils.hasText(entity.getLabelFr())) {
            entity.setLabel(entity.getLabelFr());
            changed = true;
        }
        return changed;
    }

    private static boolean patch(ReportType entity, String[] frArEn) {
        boolean changed = false;
        if (!StringUtils.hasText(entity.getLabelFr())) {
            entity.setLabelFr(frArEn[0]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabelAr())) {
            entity.setLabelAr(frArEn[1]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabelEn())) {
            entity.setLabelEn(frArEn[2]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabel()) && StringUtils.hasText(entity.getLabelFr())) {
            entity.setLabel(entity.getLabelFr());
            changed = true;
        }
        return changed;
    }

    private static boolean patch(Status entity, String[] frArEn) {
        boolean changed = false;
        if (!StringUtils.hasText(entity.getLabelFr())) {
            entity.setLabelFr(frArEn[0]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabelAr())) {
            entity.setLabelAr(frArEn[1]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabelEn())) {
            entity.setLabelEn(frArEn[2]);
            changed = true;
        }
        if (!StringUtils.hasText(entity.getLabel()) && StringUtils.hasText(entity.getLabelFr())) {
            entity.setLabel(entity.getLabelFr());
            changed = true;
        }
        return changed;
    }

    private static boolean ensureFrMirror(SupportType entity) {
        if (!StringUtils.hasText(entity.getLabelFr()) && StringUtils.hasText(entity.getLabel())) {
            entity.setLabelFr(entity.getLabel());
            return true;
        }
        return false;
    }

    private static boolean ensureFrMirror(ReportType entity) {
        if (!StringUtils.hasText(entity.getLabelFr()) && StringUtils.hasText(entity.getLabel())) {
            entity.setLabelFr(entity.getLabel());
            return true;
        }
        return false;
    }

    private static boolean ensureFrMirror(Status entity) {
        if (!StringUtils.hasText(entity.getLabelFr()) && StringUtils.hasText(entity.getLabel())) {
            entity.setLabelFr(entity.getLabel());
            return true;
        }
        return false;
    }
}
