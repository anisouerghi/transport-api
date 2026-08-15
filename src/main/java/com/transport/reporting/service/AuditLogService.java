package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditResult;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.common.util.RequestMetadata;
import com.transport.reporting.common.util.UserAgentParser;
import com.transport.reporting.dto.AuditLogCriteria;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.AuditLogResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.AuditLog;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.AuditLogMapper;
import com.transport.reporting.repository.AuditLogRepository;
import com.transport.reporting.repository.UserRepository;
import com.transport.reporting.specification.AuditLogSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service métier du journal d'audit.
 * <p>
 * Les services métier appellent {@link #record(AuditLogEvent)} pour tracer une action
 * sans code dans les contrôleurs. L'écriture s'effectue dans une transaction séparée
 * ({@code REQUIRES_NEW}) afin de ne pas perturber la transaction métier.
 */
@Service
@Slf4j
public class AuditLogService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "auditLogId",
            "auditLogId", "auditLogId",
            "actionDate", "actionDate",
            "username", "username",
            "module", "module",
            "actionType", "actionType",
            "result", "result",
            "ipAddress", "ipAddress"
    );

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final UserRepository userRepository;
    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
        this.userRepository = userRepository;
    }


    /**
     * Recherche paginée multicritère (POST /search).
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(SearchRequest<AuditLogCriteria> request) {
        AuditLogCriteria criteria = request != null ? request.getFilters() : null;
        Pageable pageable = PageableUtils.toPageable(
                request != null ? request.getPageable() : null,
                "actionDate",
                SORT_FIELDS
        );
        Specification<AuditLog> spec = AuditLogSpecification.fromCriteria(criteria);
        Page<AuditLogResponse> page = auditLogRepository.findAll(spec, pageable)
                .map(auditLogMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse findById(Long id) {
        return auditLogMapper.toResponse(auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id)));
    }

    /**
     * Enregistre une entrée d'audit. Les erreurs d'écriture sont journalisées
     * sans faire échouer l'opération métier appelante.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogEvent event) {
        if (event == null || event.getActionType() == null || event.getModule() == null) {
            log.warn("Audit event ignored: missing actionType or module");
            return;
        }
        try {
            enrichActor(event);
            String userAgent = event.getUserAgent() != null ? event.getUserAgent() : RequestMetadata.currentUserAgent();
            String ip = event.getIpAddress() != null ? event.getIpAddress() : RequestMetadata.currentIpAddress();

            AuditLog entity = AuditLog.builder()
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .userFullName(event.getUserFullName())
                    .ipAddress(truncate(ip, 64))
                    .actionType(event.getActionType())
                    .module(event.getModule())
                    .entityName(truncate(event.getEntityName(), 100))
                    .entityId(truncate(event.getEntityId(), 100))
                    .oldValue(event.getOldValue())
                    .newValue(event.getNewValue())
                    .description(truncate(event.getDescription(), 2000))
                    .userAgent(truncate(userAgent, 500))
                    .browser(UserAgentParser.detectBrowser(userAgent))
                    .operatingSystem(UserAgentParser.detectOperatingSystem(userAgent))
                    .result(event.getResult() != null ? event.getResult() : AuditResult.SUCCESS)
                    .build();

            auditLogRepository.save(entity);
        } catch (Exception ex) {
            log.error("Failed to persist audit log: {}", ex.getMessage(), ex);
        }
    }

    private void enrichActor(AuditLogEvent event) {
        if (event.getUserId() == null) {
            return;
        }
        if (event.getUsername() != null && event.getUserFullName() != null) {
            return;
        }
        userRepository.findById(event.getUserId()).ifPresent(user -> applyUser(event, user));
    }

    private void applyUser(AuditLogEvent event, AppUser user) {
        if (event.getUsername() == null) {
            event.setUsername(user.getUsername());
        }
        if (event.getUserFullName() == null) {
            event.setUserFullName(user.getName());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
