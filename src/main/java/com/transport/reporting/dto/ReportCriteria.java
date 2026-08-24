package com.transport.reporting.dto;

import com.transport.reporting.common.enums.Priority;

import java.time.Instant;

/**
 * Critères de recherche multicritère pour les signalements.
 * Tous les champs sont optionnels.
 */
public class ReportCriteria {

    private String reference;
    private String description;
    private Priority priority;
    private Long reportTypeId;
    private Long statusId;
    private String supportUuid;
    private String supportReference;
    private Instant creationDateFrom;
    private Instant creationDateTo;
    private Instant closureDateFrom;
    private Instant closureDateTo;
    /** true = au moins une réponse ; false = aucune ; null = tous. */
    private Boolean replied;
    /** Filtre nature : id technique. */
    private Long natureId;
    /** true = sans nature (non classé) ; ignore natureId si true. */
    private Boolean uncategorized;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Long getReportTypeId() {
        return reportTypeId;
    }

    public void setReportTypeId(Long reportTypeId) {
        this.reportTypeId = reportTypeId;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public String getSupportUuid() {
        return supportUuid;
    }

    public void setSupportUuid(String supportUuid) {
        this.supportUuid = supportUuid;
    }

    public String getSupportReference() {
        return supportReference;
    }

    public void setSupportReference(String supportReference) {
        this.supportReference = supportReference;
    }

    public Instant getCreationDateFrom() {
        return creationDateFrom;
    }

    public void setCreationDateFrom(Instant creationDateFrom) {
        this.creationDateFrom = creationDateFrom;
    }

    public Instant getCreationDateTo() {
        return creationDateTo;
    }

    public void setCreationDateTo(Instant creationDateTo) {
        this.creationDateTo = creationDateTo;
    }

    public Instant getClosureDateFrom() {
        return closureDateFrom;
    }

    public void setClosureDateFrom(Instant closureDateFrom) {
        this.closureDateFrom = closureDateFrom;
    }

    public Instant getClosureDateTo() {
        return closureDateTo;
    }

    public void setClosureDateTo(Instant closureDateTo) {
        this.closureDateTo = closureDateTo;
    }

    public Boolean getReplied() {
        return replied;
    }

    public void setReplied(Boolean replied) {
        this.replied = replied;
    }

    public Long getNatureId() {
        return natureId;
    }

    public void setNatureId(Long natureId) {
        this.natureId = natureId;
    }

    public Boolean getUncategorized() {
        return uncategorized;
    }

    public void setUncategorized(Boolean uncategorized) {
        this.uncategorized = uncategorized;
    }
}
