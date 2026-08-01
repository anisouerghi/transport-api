package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Permission dynamique (module × action), utilisée comme {@code GrantedAuthority}.
 * Exemple : code {@code REPORT_VIEW}, module {@code REPORT}, action {@code VIEW}.
 */
@Entity
@Table(
        name = "permission",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_permission_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_permission_module_action", columnNames = {"module_code", "action_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    /** Code unique (ex. REPORT_VIEW) — authority Spring Security. */
    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    /** Code module (ex. REPORT, USER). */
    @Column(name = "module_code", nullable = false, length = 80)
    private String moduleCode;

    /** Libellé module pour la matrice (ex. Signalements). */
    @Column(name = "module_label", nullable = false, length = 150)
    private String moduleLabel;

    /** Code action (VIEW, ADD, EDIT, DELETE…). */
    @Column(name = "action_code", nullable = false, length = 40)
    private String actionCode;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
