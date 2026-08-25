package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Nature métier d'un signalement — table report_nature.
 * Référentiel configurable (propreté, sécurité, etc.) pour statistiques.
 */
@Entity
@Table(name = "report_nature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportNature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_nature_id")
    private Long reportNatureId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
