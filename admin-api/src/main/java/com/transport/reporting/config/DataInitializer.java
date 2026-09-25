package com.transport.reporting.config;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.entity.*;
import com.transport.reporting.repository.*;
import com.transport.reporting.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Configuration
@Profile("dev")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final QrCodeService qrCodeService;

    public DataInitializer(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @Bean
    @Profile("dev")
    CommandLineRunner initData(
            StatusRepository statusRepository,
            SupportTypeRepository supportTypeRepository,
            ReportTypeRepository reportTypeRepository,
            TransportSupportRepository transportSupportRepository,
            PassengerRepository passengerRepository,
            ReportRepository reportRepository,
            DistrictRepository districtRepository) {
        return args -> {
            // ============ INITIALISATION DES STATUS ============
            if (statusRepository.count() == 0) {
                statusRepository.save(i18nStatus("NEW", "Nouveau", "جديد", "New", 1));
                statusRepository.save(i18nStatus("IN_PROGRESS", "En cours", "قيد المعالجة", "In progress", 2));
                statusRepository.save(i18nStatus("RESOLVED", "Résolu", "محلول", "Resolved", 3));
                statusRepository.save(i18nStatus("CLOSED", "Clôturé", "مغلق", "Closed", 4));
                log.info("✅ Status initialized");
            }

            // ============ INITIALISATION DES TYPES DE SUPPORT ============
            if (supportTypeRepository.count() == 0) {
                supportTypeRepository.save(i18nSupportType("BUS", "Bus", "حافلة", "Bus"));
                supportTypeRepository.save(i18nSupportType("METRO", "Métro", "مترو", "Metro"));
                supportTypeRepository.save(i18nSupportType("TRAIN", "Train", "قطار", "Train"));
                supportTypeRepository.save(i18nSupportType("STATION", "Station", "محطة", "Station"));
                log.info("✅ Support types initialized");
            }

            // ============ INITIALISATION DES TYPES DE RAPPORT ============
            // Seed de secours (dev) : ReportTypeDataInitializer assure aussi les 6 codes en prod.
            if (reportTypeRepository.count() == 0) {
                reportTypeRepository.save(i18nReportType(
                        "COMPLAINT", "Réclamation", "شكوى", "Complaint", "Réclamation voyageur"));
                reportTypeRepository.save(i18nReportType(
                        "ASSAULT", "Agression", "اعتداء", "Assault", "Signalement d'agression ou de violence"));
                reportTypeRepository.save(i18nReportType(
                        "INCIDENT", "Incident", "حادث", "Incident", "Incident technique ou sécurité"));
                reportTypeRepository.save(i18nReportType(
                        "SUGGESTION", "Suggestion", "اقتراح", "Suggestion", "Suggestion d'amélioration"));
                reportTypeRepository.save(i18nReportType(
                        "THANKS", "Remerciement", "شكر", "Thank you", "Remerciement"));
                reportTypeRepository.save(i18nReportType(
                        "OTHER", "Autre", "أخرى", "Other", "Autre nature de signalement"));
                log.info("✅ Report types initialized");
            }

            // ============ INITIALISATION DES SUPPORTS DE TRANSPORT ============
            if (transportSupportRepository.count() == 0) {
                // Récupération des références existantes
                SupportType bus = supportTypeRepository.findByCode("BUS")
                        .orElseThrow(() -> new RuntimeException("Support type BUS not found"));

                // ✅ Récupérer un district existant OU en créer un par défaut
                District district = districtRepository.findAll().stream()
                        .findFirst()
                        .orElseGet(() -> {
                            log.warn("⚠️ No district found, creating default district");
                            District defaultDistrict = District.builder()
                                    .codeDistrict("A")
                                    .libelleDistrict("TUNIS II (CHARGUIA)")
                                    .build();
                            return districtRepository.save(defaultDistrict);
                        });

                // Création du support avec tous les champs requis
                // TransportSupport n'a plus de Lombok @Builder (compat Eclipse) — setters.
                TransportSupport support = new TransportSupport();
                support.setReference("7500");
                support.setLabel("7500");
                support.setQrStatus(QrStatus.ACTIVE);
                support.setSupportStatus(SupportStatus.ACTIVE);
                support.setSupportType(bus);
                support.setDistrict(district);

                // Sauvegarde initiale
                TransportSupport savedSupport = transportSupportRepository.saveAndFlush(support);

                // Génération du QR Code
                savedSupport.setQrCodeUrl(qrCodeService.buildPublicUrl(savedSupport));
                savedSupport.setQrCodePath(qrCodeService.generateAndStore(savedSupport));

                // Sauvegarde finale
                transportSupportRepository.save(savedSupport);

                log.info("✅ Demo support created with district: {} - uuid={} url={}", 
                        district.getLibelleDistrict(), savedSupport.getUuid(), savedSupport.getQrCodeUrl());
            }

            // ============ INITIALISATION DES PASSAGERS ET RAPPORTS ============
            if (reportRepository.count() == 0) {
                seedDemoReports(
                        statusRepository,
                        reportTypeRepository,
                        transportSupportRepository,
                        passengerRepository,
                        reportRepository);
                log.info("✅ Demo reports created");
            }

            log.info("🎯 Data initialization completed successfully!");
        };
    }

    private void seedDemoReports(
            StatusRepository statusRepository,
            ReportTypeRepository reportTypeRepository,
            TransportSupportRepository transportSupportRepository,
            PassengerRepository passengerRepository,
            ReportRepository reportRepository) {

        TransportSupport support = transportSupportRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No TransportSupport available for demo reports"));

        Status statusNew = statusRepository.findByCode("NEW")
                .orElseThrow(() -> new RuntimeException("Status NEW not found"));
        Status statusInProgress = statusRepository.findByCode("IN_PROGRESS")
                .orElseThrow(() -> new RuntimeException("Status IN_PROGRESS not found"));
        Status statusResolved = statusRepository.findByCode("RESOLVED")
                .orElseThrow(() -> new RuntimeException("Status RESOLVED not found"));
        Status statusClosed = statusRepository.findByCode("CLOSED")
                .orElseThrow(() -> new RuntimeException("Status CLOSED not found"));

        ReportType incident = reportTypeRepository.findByCode("INCIDENT")
                .orElseThrow(() -> new RuntimeException("Report type INCIDENT not found"));
        ReportType complaint = reportTypeRepository.findByCode("COMPLAINT")
                .orElseThrow(() -> new RuntimeException("Report type COMPLAINT not found"));
        ReportType suggestion = reportTypeRepository.findByCode("SUGGESTION")
                .orElseThrow(() -> new RuntimeException("Report type SUGGESTION not found"));

        // Création des passagers
        Passenger p1 = passengerRepository.save(Passenger.builder()
                .name("Anis Ouerghi")
                .email("anis.benezzin@gmail.com")
                .phoneNumber("+2169988745")
                .emailVerified(true)
                .build());

        Passenger p2 = passengerRepository.save(Passenger.builder()
                .name("Alaeddine Namouchi")
                .email("alaeddine.nammouchi@transtu.tn")
                .phoneNumber("+21622555478")
                .emailVerified(false)
                .build());

        Passenger p3 = passengerRepository.save(Passenger.builder()
                .name("Rached Ben Khalifa")
                .email("benkhalifa@transtu.com")
                .phoneNumber("+21655247895")
                .emailVerified(true)
                .build());

        String today = LocalDate.now().format(DATE_FORMAT);
        Instant now = Instant.now();

        // Rapport 1 - Incident critique
        reportRepository.save(Report.builder()
                .reference("SIG-" + today + "-100001")
                .description("Porte arrière bloquée à l'arrêt République.")
                .priority(Priority.HIGH)
                .creationDate(now.minus(2, ChronoUnit.HOURS))
                .transportSupport(support)
                .reportType(incident)
                .passenger(p1)
                .status(statusNew)
                .build());

        // Rapport 2 - Réclamation
        reportRepository.save(Report.builder()
                .reference("SIG-" + today + "-100002")
                .description("Climatisation défaillante dans le véhicule.")
                .priority(Priority.MEDIUM)
                .creationDate(now.minus(1, ChronoUnit.DAYS))
                .transportSupport(support)
                .reportType(complaint)
                .passenger(p2)
                .status(statusInProgress)
                .build());

        // Rapport 3 - Suggestion
        reportRepository.save(Report.builder()
                .reference("SIG-" + today + "-100003")
                .description("Affichage Num Bus peu lisible le soir.")
                .priority(Priority.LOW)
                .creationDate(now.minus(3, ChronoUnit.DAYS))
                .transportSupport(support)
                .reportType(suggestion)
                .passenger(p3)
                .status(statusResolved)
                .closureDate(now.minus(1, ChronoUnit.DAYS))
                .build());

        // Rapport 4 - Incident critique
        reportRepository.save(Report.builder()
                .reference("SIG-" + today + "-100004")
                .description("Comportement agressif signalé à bord.")
                .priority(Priority.CRITICAL)
                .creationDate(now.minus(5, ChronoUnit.DAYS))
                .transportSupport(support)
                .reportType(incident)
                .passenger(p1)
                .status(statusClosed)
                .closureDate(now.minus(4, ChronoUnit.DAYS))
                .build());

        // Rapport 5 - Réclamation récente
        reportRepository.save(Report.builder()
                .reference("SIG-" + today + "-100005")
                .description("Retard important sans information voyageurs.")
                .priority(Priority.MEDIUM)
                .creationDate(now.minus(6, ChronoUnit.HOURS))
                .transportSupport(support)
                .reportType(complaint)
                .passenger(p2)
                .status(statusNew)
                .build());

        log.info("✅ {} demo reports created", reportRepository.count());
    }

    private static Status i18nStatus(String code, String fr, String ar, String en, int order) {
        return Status.builder()
                .code(code)
                .label(fr)
                .labelFr(fr)
                .labelAr(ar)
                .labelEn(en)
                .displayOrder(order)
                .build();
    }

    private static SupportType i18nSupportType(String code, String fr, String ar, String en) {
        return SupportType.builder()
                .code(code)
                .label(fr)
                .labelFr(fr)
                .labelAr(ar)
                .labelEn(en)
                .build();
    }

    private static ReportType i18nReportType(String code, String fr, String ar, String en, String description) {
        return ReportType.builder()
                .code(code)
                .label(fr)
                .labelFr(fr)
                .labelAr(ar)
                .labelEn(en)
                .description(description)
                .active(true)
                .build();
    }
}