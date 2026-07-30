package com.transport.reporting.config;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.Report;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.repository.PassengerRepository;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Initialisation des donnees de demonstration (profil dev).
 */
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
            UserRepository userRepository,
            PassengerRepository passengerRepository,
            ReportRepository reportRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (statusRepository.count() == 0) {
                statusRepository.save(Status.builder().code("NEW").label("Nouveau").displayOrder(1).build());
                statusRepository.save(Status.builder().code("IN_PROGRESS").label("En cours").displayOrder(2).build());
                statusRepository.save(Status.builder().code("RESOLVED").label("Résolu").displayOrder(3).build());
                statusRepository.save(Status.builder().code("CLOSED").label("Clôturé").displayOrder(4).build());
            }

            if (supportTypeRepository.count() == 0) {
                supportTypeRepository.save(SupportType.builder().code("BUS").label("Bus").build());
                supportTypeRepository.save(SupportType.builder().code("METRO").label("Métro").build());
                supportTypeRepository.save(SupportType.builder().code("TRAIN").label("Train").build());
                supportTypeRepository.save(SupportType.builder().code("STATION").label("Station").build());
            }

            if (reportTypeRepository.count() == 0) {
                reportTypeRepository.save(ReportType.builder()
                        .code("INCIDENT").label("Incident").description("Incident technique ou sécurité").build());
                reportTypeRepository.save(ReportType.builder()
                        .code("COMPLAINT").label("Réclamation").description("Réclamation voyageur").build());
                reportTypeRepository.save(ReportType.builder()
                        .code("SUGGESTION").label("Suggestion").description("Suggestion d'amélioration").build());
            }

            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(AppUser.builder()
                        .username("admin")
                        .name("Administrator")
                        .email("admin@transport.transtu.tn")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .active(true)
                        .build());
                log.info("Admin user created (admin / admin123)");
            }

            if (transportSupportRepository.count() == 0) {
                SupportType bus = supportTypeRepository.findByCode("BUS").orElseThrow();
                TransportSupport support = transportSupportRepository.saveAndFlush(TransportSupport.builder()
                        .reference("7500")
                        .label("7500")
                        .qrStatus(QrStatus.ACTIVE)
                        .supportStatus(SupportStatus.ACTIVE)
                        .supportType(bus)
                        .build());
                support.setQrCodeUrl(qrCodeService.buildPublicUrl(support));
                support.setQrCodePath(qrCodeService.generateAndStore(support));
                transportSupportRepository.save(support);
                log.info("Demo support created uuid={} url={}", support.getUuid(), support.getQrCodeUrl());
            }

            if (reportRepository.count() == 0) {
                seedDemoReports(
                        statusRepository,
                        reportTypeRepository,
                        transportSupportRepository,
                        passengerRepository,
                        reportRepository);
            }
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

        Status statusNew = statusRepository.findByCode("NEW").orElseThrow();
        Status statusInProgress = statusRepository.findByCode("IN_PROGRESS").orElseThrow();
        Status statusResolved = statusRepository.findByCode("RESOLVED").orElseThrow();
        Status statusClosed = statusRepository.findByCode("CLOSED").orElseThrow();

        ReportType incident = reportTypeRepository.findByCode("INCIDENT").orElseThrow();
        ReportType complaint = reportTypeRepository.findByCode("COMPLAINT").orElseThrow();
        ReportType suggestion = reportTypeRepository.findByCode("SUGGESTION").orElseThrow();

        Passenger p1 = passengerRepository.save(Passenger.builder()
                .name("Anis Ouerghi")
                .email("anis.ourghi@transtu.tn")
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
                .name("Rached")
                .email("benkhalifa@transtu.com")
                .phoneNumber("+21655247895")
                .emailVerified(true)
                .build());

        String today = LocalDate.now().format(DATE_FORMAT);
        Instant now = Instant.now();

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

        log.info("Demo reports created: {}", reportRepository.count());
    }
}
