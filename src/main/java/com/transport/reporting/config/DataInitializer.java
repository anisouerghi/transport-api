package com.transport.reporting.config;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.entity.*;
import com.transport.reporting.repository.*;
import com.transport.reporting.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final QrCodeService qrCodeService;

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
                statusRepository.save(Status.builder().code("NEW").label("Nouveau").displayOrder(1).build());
                statusRepository.save(Status.builder().code("IN_PROGRESS").label("En cours").displayOrder(2).build());
                statusRepository.save(Status.builder().code("RESOLVED").label("Résolu").displayOrder(3).build());
                statusRepository.save(Status.builder().code("CLOSED").label("Clôturé").displayOrder(4).build());
                log.info("✅ Status initialized");
            }

            // ============ INITIALISATION DES TYPES DE SUPPORT ============
            if (supportTypeRepository.count() == 0) {
                supportTypeRepository.save(SupportType.builder().code("BUS").label("Bus").build());
                supportTypeRepository.save(SupportType.builder().code("METRO").label("Métro").build());
                supportTypeRepository.save(SupportType.builder().code("TRAIN").label("Train").build());
                supportTypeRepository.save(SupportType.builder().code("STATION").label("Station").build());
                log.info("✅ Support types initialized");
            }

            // ============ INITIALISATION DES TYPES DE RAPPORT ============
            if (reportTypeRepository.count() == 0) {
                reportTypeRepository.save(ReportType.builder()
                        .code("INCIDENT").label("Incident").description("Incident technique ou sécurité").build());
                reportTypeRepository.save(ReportType.builder()
                        .code("COMPLAINT").label("Réclamation").description("Réclamation voyageur").build());
                reportTypeRepository.save(ReportType.builder()
                        .code("SUGGESTION").label("Suggestion").description("Suggestion d'amélioration").build());
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
                TransportSupport support = TransportSupport.builder()
                        .reference("7500")
                        .label("7500")
                        .qrStatus(QrStatus.ACTIVE)
                        .supportStatus(SupportStatus.ACTIVE)
                        .supportType(bus)
                        .district(district) // ✅ DISTRICT OBLIGATOIRE
                        .build();

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
                .name("Alaa Nammouchi")
                .email("alaa.namouchi@transtu.tn")
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
}