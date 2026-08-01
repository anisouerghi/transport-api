package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entrée de menu administration, filtrée dynamiquement par permission.
 */
@Entity
@Table(name = "app_menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Column(name = "icon", length = 80)
    private String icon;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /** Code permission requis ; null = visible pour tout utilisateur authentifié. */
    @Column(name = "permission_code", length = 80)
    private String permissionCode;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
