package com.transport.reporting.modules.utilisateur.entity;

import com.transport.reporting.common.enums.RoleUtilisateur;
import jakarta.persistence.*;
import lombok.*;

/**
 * Agent ou administrateur interne.
 */
@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleUtilisateur role;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
