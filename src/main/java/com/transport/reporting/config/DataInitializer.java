package com.transport.reporting.config;

import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Status;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.StatusRepository;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    @Bean
    @Profile("dev")
    CommandLineRunner initData(
            StatusRepository statusRepository,
            SupportTypeRepository supportTypeRepository,
            ReportTypeRepository reportTypeRepository,
            TransportSupportRepository transportSupportRepository,
            UserRepository userRepository,
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
                        .email("admin@transport.local")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .build());
                log.info("Admin user created (admin / admin123)");
            }

            if (transportSupportRepository.count() == 0) {
                SupportType bus = supportTypeRepository.findByCode("BUS").orElseThrow();
                TransportSupport support = transportSupportRepository.save(TransportSupport.builder()
                        .reference("BUS-L12-4521")
                        .label("Bus Line 12 - Vehicle 4521")
                        .qrCodeUrl("https://app.transport.local/q/demo")
                        .qrStatus(QrStatus.ACTIVE)
                        .supportStatus(SupportStatus.ACTIVE)
                        .supportType(bus)
                        .build());
                log.info("Demo support created uuid={}", support.getUuid());
            }
        };
    }
}
