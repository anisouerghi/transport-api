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
import lombok.RequiredArgsConstructor;
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
 */
@Service
@RequiredArgsConstructor
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
            log.debug("Recording audit event: {} - {}", event.getActionType(), event.getModule());
            
            enrichActor(event);
            
            // Récupérer les métadonnées avec gestion des null
            String userAgent = event.getUserAgent();
            if (userAgent == null || userAgent.isBlank()) {
                try {
                    userAgent = RequestMetadata.currentUserAgent();
                } catch (Exception e) {
                    log.debug("Could not get User-Agent: {}", e.getMessage());
                    userAgent = "unknown";
                }
            }
            
            String ip = event.getIpAddress();
            if (ip == null || ip.isBlank()) {
                try {
                    ip = RequestMetadata.currentIpAddress();
                } catch (Exception e) {
                    log.debug("Could not get IP address: {}", e.getMessage());
                    ip = "0.0.0.0";
                }
            }

            // Assurer que les valeurs ne sont pas null
            String finalUserAgent = userAgent != null ? userAgent : "unknown";
            String finalIp = ip != null ? ip : "0.0.0.0";
            
            // Construire l'entité avec des valeurs sécurisées
            AuditLog entity = AuditLog.builder()
                    .userId(event.getUserId())
                    .username(safeTruncate(event.getUsername(), 255))
                    .userFullName(safeTruncate(event.getUserFullName(), 255))
                    .ipAddress(safeTruncate(finalIp, 64))
                    .actionType(event.getActionType())
                    .module(event.getModule())
                    .entityName(safeTruncate(event.getEntityName(), 100))
                    .entityId(safeTruncate(event.getEntityId(), 100))
                    .oldValue(event.getOldValue())
                    .newValue(event.getNewValue())
                    .description(safeTruncate(event.getDescription(), 2000))
                    .userAgent(safeTruncate(finalUserAgent, 500))
                    .browser(safeTruncate(UserAgentParser.detectBrowser(finalUserAgent), 50))
                    .operatingSystem(safeTruncate(UserAgentParser.detectOperatingSystem(finalUserAgent), 50))
                    .result(event.getResult() != null ? event.getResult() : AuditResult.SUCCESS)
                    .build();

            auditLogRepository.save(entity);
            log.debug("Audit event recorded successfully with ID: {}", entity.getAuditLogId());
            
        } catch (Exception ex) {
            // Ne pas propager l'exception pour ne pas bloquer l'opération métier
            log.error("Failed to persist audit log: {}", ex.getMessage(), ex);
        }
    }

    private void enrichActor(AuditLogEvent event) {
        if (event.getUserId() == null) {
            log.debug("No user ID provided for audit event");
            return;
        }
        if (event.getUsername() != null && event.getUserFullName() != null) {
            return;
        }
        try {
            userRepository.findById(event.getUserId()).ifPresent(user -> {
                if (event.getUsername() == null) {
                    event.setUsername(user.getUsername());
                }
                if (event.getUserFullName() == null) {
                    // Vérifiez le nom du champ dans AppUser (name, fullName, etc.)
                    event.setUserFullName(user.getName() != null ? user.getName() : user.getUsername());
                }
            });
        } catch (Exception e) {
            log.warn("Could not enrich actor info for userId {}: {}", event.getUserId(), e.getMessage());
        }
    }

    private String safeTruncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}