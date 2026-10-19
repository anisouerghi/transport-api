package com.transport.reporting.config;

import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.service.PublicStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Complète les libellés i18n et {@code statut_public} du catalogue (idempotent).
 * Insère les statuts manquants (AFFECTE, REJETE, DEMANDE_COMPLEMENT, …).
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

    /** code → [labelFr, labelAr, labelEn, displayOrder] */
    private static final Map<String, String[]> STATUSES = new LinkedHashMap<>();

    static {
        STATUSES.put("NEW", new String[]{"Nouveau", "جديد", "New", "1"});
        STATUSES.put("AFFECTE", new String[]{"Affecté", "مُسند", "Assigned", "2"});
        STATUSES.put("IN_PROGRESS", new String[]{"En cours", "قيد المعالجة", "In progress", "3"});
        STATUSES.put("DEMANDE_COMPLEMENT",
                new String[]{"Demande de complément", "طلب استكمال", "Request for additional information", "4"});
        STATUSES.put("DEMANDE_CLARIFICATION",
                new String[]{"Demande de clarification", "طلب توضيح", "Request for clarification", "5"});
        STATUSES.put("RESOLVED", new String[]{"Résolu", "محلول", "Resolved", "6"});
        STATUSES.put("CLOSED", new String[]{"Clôturé", "مغلق", "Closed", "7"});
        STATUSES.put("REJETE", new String[]{"Rejeté", "مرفوض", "Rejected", "8"});
    }

    @Bean
    @Order(3)
    CommandLineRunner patchCatalogI18n(
            SupportTypeRepository supportTypeRepository,
            StatusRepository statusRepository,
            PublicStatusMapper publicStatusMapper) {
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
            // Insert manquants puis patch labels / statut_public.
            for (Map.Entry<String, String[]> entry : STATUSES.entrySet()) {
                String code = entry.getKey();
                String[] t = entry.getValue();
                if (statusRepository.findByCode(code).isEmpty()) {
                    Status created = Status.builder()
                            .code(code)
                            .label(t[0])
                            .labelFr(t[0])
                            .labelAr(t[1])
                            .labelEn(t[2])
                            .displayOrder(Integer.parseInt(t[3]))
                            .statutPublic(publicStatusMapper.publicLabel(code))
                            .build();
                    statusRepository.save(created);
                    n++;
                    log.info("Statut catalogue créé : {}", code);
                }
            }
            for (Status entity : statusRepository.findAll()) {
                String[] t = STATUSES.get(entity.getCode());
                boolean changed = false;
                if (t != null) {
                    changed = patch(entity, t);
                } else {
                    changed = ensureFrMirror(entity);
                }
                String expectedPublic = publicStatusMapper.publicLabel(entity.getCode());
                if (StringUtils.hasText(expectedPublic)
                        && !expectedPublic.equals(entity.getStatutPublic())) {
                    entity.setStatutPublic(expectedPublic);
                    changed = true;
                }
                if (changed) {
                    statusRepository.save(entity);
                    n++;
                }
            }
            log.info("Catalog i18n / statut_public patched ({} rows)", n);
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

    private static boolean ensureFrMirror(Status entity) {
        if (!StringUtils.hasText(entity.getLabelFr()) && StringUtils.hasText(entity.getLabel())) {
            entity.setLabelFr(entity.getLabel());
            return true;
        }
        return false;
    }
}
