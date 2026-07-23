package com.transport.reporting.modules.report.entity;

import com.transport.reporting.modules.status.entity.Status;
import com.transport.reporting.modules.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "REPORT_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historyId")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oldStatusId")
    private Status oldStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "newStatusId", nullable = false)
    private Status newStatus;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "actionDate", nullable = false)
    private Instant actionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportId", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private AppUser user;

    @PrePersist
    public void prePersist() {
        if (actionDate == null) {
            actionDate = Instant.now();
        }
    }
}
