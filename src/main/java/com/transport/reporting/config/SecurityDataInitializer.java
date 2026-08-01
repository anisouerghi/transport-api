package com.transport.reporting.config;

import com.transport.reporting.entity.AppMenu;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Permission;
import com.transport.reporting.entity.Role;
import com.transport.reporting.repository.AppMenuRepository;
import com.transport.reporting.repository.PermissionRepository;
import com.transport.reporting.repository.RoleRepository;
import com.transport.reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seed sécurité : matrice de permissions (module × action), rôles, menus, admin.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityDataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner initSecurityData(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            AppMenuRepository appMenuRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            seedPermissions(permissionRepository);
            seedRoles(permissionRepository, roleRepository);
            seedMenus(appMenuRepository);
            seedAdminUser(userRepository, roleRepository, passwordEncoder);
        };
    }

    private void seedPermissions(PermissionRepository permissionRepository) {
        if (permissionRepository.count() > 0) {
            return;
        }
        List<Permission> permissions = List.of(
                // Dashboard
                p("DASHBOARD", "Dashboard", "VIEW", "Consulter le tableau de bord"),

                // Signalements
                p("REPORT", "Signalements", "VIEW", "Consulter les signalements"),
                p("REPORT", "Signalements", "SEARCH", "Rechercher les signalements"),
                p("REPORT", "Signalements", "EXPORT", "Exporter les signalements"),
                p("REPORT", "Signalements", "PRINT", "Imprimer un signalement"),
                p("REPORT", "Signalements", "REPLY", "Répondre à un signalement"),
                p("REPORT", "Signalements", "ASSIGN", "Affecter un signalement"),
                p("REPORT", "Signalements", "CLOSE", "Clôturer / changer le statut"),
                p("REPORT", "Signalements", "EDIT", "Modifier un signalement"),

                // Types de signalement
                p("REPORT_TYPE", "Types de signalement", "VIEW", "Consulter les types"),
                p("REPORT_TYPE", "Types de signalement", "ADD", "Créer un type"),
                p("REPORT_TYPE", "Types de signalement", "EDIT", "Modifier un type"),
                p("REPORT_TYPE", "Types de signalement", "DELETE", "Supprimer un type"),
                p("REPORT_TYPE", "Types de signalement", "SEARCH", "Rechercher les types"),
                p("REPORT_TYPE", "Types de signalement", "ACTIVATE", "Activer un type"),
                p("REPORT_TYPE", "Types de signalement", "DEACTIVATE", "Désactiver un type"),

                // Types de support
                p("SUPPORT_TYPE", "Types de support", "VIEW", "Consulter les types de support"),
                p("SUPPORT_TYPE", "Types de support", "ADD", "Créer un type de support"),
                p("SUPPORT_TYPE", "Types de support", "EDIT", "Modifier un type de support"),
                p("SUPPORT_TYPE", "Types de support", "DELETE", "Supprimer un type de support"),
                p("SUPPORT_TYPE", "Types de support", "SEARCH", "Rechercher les types de support"),

                // Supports transport
                p("TRANSPORT_SUPPORT", "Supports transport", "VIEW", "Consulter les supports"),
                p("TRANSPORT_SUPPORT", "Supports transport", "ADD", "Créer un support"),
                p("TRANSPORT_SUPPORT", "Supports transport", "EDIT", "Modifier un support"),
                p("TRANSPORT_SUPPORT", "Supports transport", "DELETE", "Supprimer un support"),
                p("TRANSPORT_SUPPORT", "Supports transport", "SEARCH", "Rechercher les supports"),
                p("TRANSPORT_SUPPORT", "Supports transport", "PRINT", "Imprimer / télécharger le QR"),
                p("TRANSPORT_SUPPORT", "Supports transport", "ACTIVATE", "Activer un support"),
                p("TRANSPORT_SUPPORT", "Supports transport", "DEACTIVATE", "Désactiver un support"),

                // Utilisateurs
                p("USER", "Utilisateurs", "VIEW", "Consulter les utilisateurs"),
                p("USER", "Utilisateurs", "ADD", "Créer un utilisateur"),
                p("USER", "Utilisateurs", "EDIT", "Modifier un utilisateur"),
                p("USER", "Utilisateurs", "DELETE", "Supprimer un utilisateur"),
                p("USER", "Utilisateurs", "SEARCH", "Rechercher les utilisateurs"),
                p("USER", "Utilisateurs", "EXPORT", "Exporter les utilisateurs"),
                p("USER", "Utilisateurs", "ACTIVATE", "Activer un utilisateur"),
                p("USER", "Utilisateurs", "DEACTIVATE", "Désactiver un utilisateur"),

                // Rôles
                p("ROLE", "Rôles", "VIEW", "Consulter les rôles"),
                p("ROLE", "Rôles", "ADD", "Créer un rôle"),
                p("ROLE", "Rôles", "EDIT", "Modifier un rôle"),
                p("ROLE", "Rôles", "DELETE", "Supprimer un rôle"),
                p("ROLE", "Rôles", "SEARCH", "Rechercher les rôles"),
                p("ROLE", "Rôles", "ACTIVATE", "Activer un rôle"),
                p("ROLE", "Rôles", "DEACTIVATE", "Désactiver un rôle"),

                // Permissions
                p("PERMISSION", "Permissions", "VIEW", "Consulter les permissions"),
                p("PERMISSION", "Permissions", "SEARCH", "Rechercher les permissions"),

                // Journal d'audit
                p("AUDIT", "Journal d'audit", "VIEW", "Consulter le journal d'audit"),
                p("AUDIT", "Journal d'audit", "SEARCH", "Rechercher dans l'audit"),
                p("AUDIT", "Journal d'audit", "EXPORT", "Exporter l'audit"),

                // Statuts
                p("STATUS", "Statuts", "VIEW", "Consulter les statuts"),
                p("STATUS", "Statuts", "ADD", "Créer un statut"),
                p("STATUS", "Statuts", "EDIT", "Modifier un statut"),
                p("STATUS", "Statuts", "DELETE", "Supprimer un statut")
        );
        permissionRepository.saveAll(permissions);
        log.info("Permission matrix seeded ({} entries)", permissions.size());
    }

    private void seedRoles(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        if (roleRepository.count() > 0) {
            return;
        }
        Map<String, Permission> byCode = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, x -> x));

        Role admin = Role.builder()
                .code("ADMIN")
                .label("Administrateur")
                .description("Accès complet")
                .active(true)
                .permissions(new HashSet<>(byCode.values()))
                .build();
        roleRepository.save(admin);

        Role agent = Role.builder()
                .code("AGENT")
                .label("Agent")
                .description("Traitement des signalements")
                .active(true)
                .permissions(codes(byCode,
                        "DASHBOARD_VIEW",
                        "REPORT_VIEW", "REPORT_SEARCH", "REPORT_REPLY", "REPORT_CLOSE", "REPORT_EDIT",
                        "TRANSPORT_SUPPORT_VIEW", "TRANSPORT_SUPPORT_SEARCH", "TRANSPORT_SUPPORT_PRINT",
                        "STATUS_VIEW"))
                .build();
        roleRepository.save(agent);

        Role responsable = Role.builder()
                .code("RESPONSABLE")
                .label("Responsable")
                .description("Supervision")
                .active(true)
                .permissions(codes(byCode,
                        "DASHBOARD_VIEW",
                        "REPORT_VIEW", "REPORT_SEARCH", "REPORT_EXPORT", "REPORT_PRINT",
                        "REPORT_REPLY", "REPORT_ASSIGN", "REPORT_CLOSE", "REPORT_EDIT",
                        "REPORT_TYPE_VIEW", "REPORT_TYPE_ADD", "REPORT_TYPE_EDIT", "REPORT_TYPE_SEARCH",
                        "REPORT_TYPE_ACTIVATE", "REPORT_TYPE_DEACTIVATE",
                        "SUPPORT_TYPE_VIEW", "SUPPORT_TYPE_ADD", "SUPPORT_TYPE_EDIT", "SUPPORT_TYPE_SEARCH",
                        "TRANSPORT_SUPPORT_VIEW", "TRANSPORT_SUPPORT_ADD", "TRANSPORT_SUPPORT_EDIT",
                        "TRANSPORT_SUPPORT_SEARCH", "TRANSPORT_SUPPORT_PRINT",
                        "TRANSPORT_SUPPORT_ACTIVATE", "TRANSPORT_SUPPORT_DEACTIVATE",
                        "AUDIT_VIEW", "AUDIT_SEARCH", "AUDIT_EXPORT",
                        "STATUS_VIEW"))
                .build();
        roleRepository.save(responsable);

        log.info("Roles seeded (ADMIN, AGENT, RESPONSABLE)");
    }

    private void seedMenus(AppMenuRepository appMenuRepository) {
        if (appMenuRepository.count() > 0) {
            return;
        }
        appMenuRepository.saveAll(List.of(
                menu("DASHBOARD", "Dashboard", "/dashboard", "cilSpeedometer", 10, "DASHBOARD_VIEW"),
                menu("REPORTS", "Signalements", "/reports", "cilList", 20, "REPORT_VIEW"),
                menu("TRANSPORT_SUPPORTS", "Supports", "/transport-supports", "cilList", 30, "TRANSPORT_SUPPORT_VIEW"),
                menu("SUPPORT_TYPES", "Types de support", "/support-types", "cilList", 40, "SUPPORT_TYPE_VIEW"),
                menu("REPORT_TYPES", "Types de signalement", "/report-types", "cilSpeech", 50, "REPORT_TYPE_VIEW"),
                menu("USERS", "Utilisateurs", "/users", "cilUser", 60, "USER_VIEW"),
                menu("ROLES", "Rôles", "/roles", "cilLockLocked", 70, "ROLE_VIEW"),
                menu("PERMISSIONS", "Permissions", "/permissions", "cilLockLocked", 80, "PERMISSION_VIEW"),
                menu("AUDIT", "Journal d'audit", "/audit-logs", "cilHistory", 90, "AUDIT_VIEW")
        ));
        log.info("Admin menus seeded");
    }

    private void seedAdminUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        Role adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
        AppUser admin = userRepository.findByUsernameWithRolesAndPermissions("admin")
                .orElseGet(() -> AppUser.builder()
                        .username("admin")
                        .name("Administrator")
                        .email("admin@transport.transtu.tn")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .active(true)
                        .build());
        if (admin.getRoles() == null) {
            admin.setRoles(new HashSet<>());
        }
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
        log.info("Admin user ready (admin / admin123) with role ADMIN");
    }

    private static Permission p(String module, String moduleLabel, String action, String label) {
        String code = module + "_" + action;
        return Permission.builder()
                .code(code)
                .label(label)
                .description(label)
                .moduleCode(module)
                .moduleLabel(moduleLabel)
                .actionCode(action)
                .active(true)
                .build();
    }

    private static Set<Permission> codes(Map<String, Permission> byCode, String... codes) {
        return Arrays.stream(codes)
                .map(byCode::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static AppMenu menu(String code, String label, String url, String icon, int order, String permission) {
        return AppMenu.builder()
                .code(code)
                .label(label)
                .url(url)
                .icon(icon)
                .displayOrder(order)
                .permissionCode(permission)
                .active(true)
                .build();
    }
}
