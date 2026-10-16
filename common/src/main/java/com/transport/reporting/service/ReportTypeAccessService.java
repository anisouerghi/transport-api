package com.transport.reporting.service;

import com.transport.reporting.entity.Report;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.repository.RoleRepository;
import com.transport.reporting.security.ReportTypeScope;
import com.transport.reporting.security.SecurityUtils;
import com.transport.reporting.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Détermine et applique le filtre des types de signalement autorisés pour un agent.
 * Union des types des rôles actifs ; rôle {@code ADMIN} = accès global.
 */
@Service
@Transactional(readOnly = true)
public class ReportTypeAccessService {

    public static final String ADMIN_ROLE_CODE = "ADMIN";

    private final RoleRepository roleRepository;

    public ReportTypeAccessService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public ReportTypeScope currentScope() {
        Optional<UserPrincipal> user = SecurityUtils.currentUser();
        if (user.isEmpty()) {
            return ReportTypeScope.none();
        }
        if (user.get().getRoles() != null && user.get().getRoles().contains(ADMIN_ROLE_CODE)) {
            return ReportTypeScope.unrestricted();
        }
        Long userId = user.get().getUserId();
        if (userId == null) {
            return ReportTypeScope.none();
        }
        Set<Long> ids = new LinkedHashSet<>(roleRepository.findAuthorizedReportTypeIdsByUserId(userId));
        return ReportTypeScope.of(ids);
    }

    public void requireAccess(Report report) {
        if (report == null) {
            throw new BusinessException("Signalement introuvable.", "REPORT_TYPE_FORBIDDEN");
        }
        Long typeId = report.getReportType() == null ? null : report.getReportType().getReportTypeId();
        if (!currentScope().allows(typeId)) {
            throw new BusinessException(
                    "Vous n'êtes pas autorisé à accéder à ce signalement (type non affecté à vos rôles).",
                    "REPORT_TYPE_FORBIDDEN");
        }
    }

    public <T> Specification<T> asSpecification() {
        ReportTypeScope scope = currentScope();
        if (scope.isUnrestricted()) {
            return (root, query, cb) -> cb.conjunction();
        }
        if (scope.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("reportType").get("reportTypeId").in(scope.reportTypeIds());
    }

    public SqlFilter sqlFilter(String reportAlias) {
        ReportTypeScope scope = currentScope();
        if (scope.isUnrestricted()) {
            return new SqlFilter("", java.util.List.of());
        }
        String alias = reportAlias == null || reportAlias.isBlank() ? "r" : reportAlias.trim();
        if (scope.isEmpty()) {
            return new SqlFilter(" AND 1 = 0", java.util.List.of());
        }
        String placeholders = String.join(",", scope.reportTypeIds().stream().map(id -> "?").toList());
        return new SqlFilter(
                " AND " + alias + ".report_type_id IN (" + placeholders + ")",
                java.util.List.copyOf(scope.reportTypeIds()));
    }

    public record SqlFilter(String sql, java.util.List<Object> args) {
    }
}