package com.transport.reporting.modules.report.entity;

import com.transport.reporting.common.enums.Priority;
import com.transport.reporting.modules.passenger.entity.Passenger;
import com.transport.reporting.modules.status.entity.Status;
import com.transport.reporting.modules.support.entity.TransportSupport;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "REPORT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reportId")
    private Long reportId;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private UUID uuid;

    @Column(name = "reference", nullable = false, unique = true, length = 40)
    private String reference;

    @Column(name = "creationDate", nullable = false, updatable = false)
    private Instant creationDate;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 30)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "closureDate")
    private Instant closureDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transportSupportId", nullable = false)
    private TransportSupport transportSupport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportTypeId", nullable = false)
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passengerId", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statusId", nullable = false)
    private Status status;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (creationDate == null) {
            creationDate = Instant.now();
        }
    }
}
