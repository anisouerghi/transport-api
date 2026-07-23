package com.transport.reporting.config;

import com.transport.reporting.common.enums.RoleUtilisateur;
import com.transport.reporting.common.enums.TypeSupport;
import com.transport.reporting.modules.support.entity.Support;
import com.transport.reporting.modules.support.repository.SupportRepository;
import com.transport.reporting.modules.utilisateur.entity.Utilisateur;
import com.transport.reporting.modules.utilisateur.repository.UtilisateurRepository;
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
    CommandLineRunner initDevData(
            UtilisateurRepository utilisateurRepository,
            SupportRepository supportRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (!utilisateurRepository.existsByLogin("admin")) {
                utilisateurRepository.save(Utilisateur.builder()
                        .login("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .nom("Administrateur")
                        .email("admin@transport.local")
                        .role(RoleUtilisateur.ADMINISTRATEUR)
                        .actif(true)
                        .build());
                log.info("Utilisateur admin créé (login=admin / password=admin123)");
            }

            if (supportRepository.count() == 0) {
                Support support = supportRepository.save(Support.builder()
                        .reference("BUS-L12-4521")
                        .libelle("Bus Ligne 12 - Véhicule 4521")
                        .type(TypeSupport.BUS)
                        .qrCodeUrl("https://app.transport.local/q/demo")
                        .actif(true)
                        .build());
                log.info("Support de démonstration créé : uuid={}", support.getUuid());
            }
        };
    }
}
