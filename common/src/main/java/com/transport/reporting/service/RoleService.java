package com.transport.reporting.service;

import com.transport.reporting.dto.PermissionMatrixResponse;
import com.transport.reporting.dto.PermissionResponse;
import com.transport.reporting.dto.RoleRequest;
import com.transport.reporting.dto.RoleResponse;
import com.transport.reporting.entity.Permission;
import com.transport.reporting.entity.ReportType;
import com.transport.reporting.entity.Role;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.RoleMapper;
import com.transport.reporting.repository.PermissionRepository;
import com.transport.reporting.repository.ReportTypeRepository;
import com.transport.reporting.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ReportTypeRepository reportTypeRepository;
    private final RoleMapper roleMapper;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            ReportTypeRepository reportTypeRepository,
            RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.reportTypeRepository = reportTypeRepository;
        this.roleMapper = roleMapper;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        List<Role> roles = roleRepository.findAllWithPermissions();
        hydrateReportTypes(roles);
        return roles.stream().map(roleMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return roleMapper.toResponse(getWithPermissionsAndTypes(id));
    }

    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Role code already exists");
        }
        Role role = Role.builder()
                .code(request.getCode().trim().toUpperCase())
                .label(request.getLabel())
                .description(request.getDescription())
                .active(request.getActive() == null || request.getActive())
                .permissions(resolvePermissions(request.getPermissionIds()))
                .reportTypes(resolveReportTypes(request.getReportTypeIds(), Set.of()))
                .build();
        return roleMapper.toResponse(roleRepository.save(role));
    }

    public RoleResponse update(Long id, RoleRequest request) {
        Role role = getWithPermissionsAndTypes(id);
        if (!role.getCode().equalsIgnoreCase(request.getCode())
                && roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("Role code already exists");
        }
        Set<Long> previouslyAssigned = role.getReportTypes() == null
                ? Set.of()
                : role.getReportTypes().stream()
                        .map(ReportType::getReportTypeId)
                        .collect(Collectors.toSet());
        role.setCode(request.getCode().trim().toUpperCase());
        role.setLabel(request.getLabel());
        role.setDescription(request.getDescription());
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }
        role.setPermissions(resolvePermissions(request.getPermissionIds()));
        role.setReportTypes(resolveReportTypes(request.getReportTypeIds(), previouslyAssigned));
        return roleMapper.toResponse(roleRepository.save(role));
    }

    public void delete(Long id) {
        Role role = getWithPermissionsAndTypes(id);
        roleRepository.delete(role);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAllByOrderByModuleCodeAscActionCodeAsc().stream()
                .map(roleMapper::toPermissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PermissionMatrixResponse getPermissionMatrix() {
        List<Permission> all = permissionRepository.findAllByOrderByModuleCodeAscActionCodeAsc();

        List<String> preferred = List.of(
                "VIEW", "ADD", "EDIT", "DELETE", "SEARCH", "EXPORT", "PRINT",
                "REPLY", "ASSIGN", "CLOSE", "ACTIVATE", "DEACTIVATE");
        LinkedHashSet<String> actionOrder = new LinkedHashSet<>(preferred);
        for (Permission p : all) {
            actionOrder.add(p.getActionCode());
        }
        List<String> usedActions = actionOrder.stream()
                .filter(a -> all.stream().anyMatch(p -> a.equals(p.getActionCode())))
                .collect(Collectors.toList());

        Map<String, String> moduleLabels = new LinkedHashMap<>();
        Map<String, Map<String, PermissionMatrixResponse.PermissionCell>> cellsByModule = new LinkedHashMap<>();

        for (Permission p : all) {
            moduleLabels.putIfAbsent(p.getModuleCode(), p.getModuleLabel());
            cellsByModule
                    .computeIfAbsent(p.getModuleCode(), k -> new LinkedHashMap<>())
                    .put(p.getActionCode(), PermissionMatrixResponse.PermissionCell.builder()
                            .permissionId(p.getPermissionId())
                            .code(p.getCode())
                            .label(p.getLabel())
                            .active(p.isActive())
                            .build());
        }

        List<PermissionMatrixResponse.ModuleRow> modules = new ArrayList<>();
        for (Map.Entry<String, String> e : moduleLabels.entrySet()) {
            Map<String, PermissionMatrixResponse.PermissionCell> cells =
                    cellsByModule.getOrDefault(e.getKey(), Map.of());
            Map<String, PermissionMatrixResponse.PermissionCell> ordered = new LinkedHashMap<>();
            for (String action : usedActions) {
                if (cells.containsKey(action)) {
                    ordered.put(action, cells.get(action));
                }
            }
            modules.add(PermissionMatrixResponse.ModuleRow.builder()
                    .moduleCode(e.getKey())
                    .moduleLabel(e.getValue())
                    .permissions(ordered)
                    .build());
        }

        return PermissionMatrixResponse.builder()
                .actions(usedActions)
                .modules(modules)
                .build();
    }

    private Role getWithPermissionsAndTypes(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        roleRepository.findByIdWithReportTypes(id);
        return role;
    }

    private void hydrateReportTypes(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        List<Long> ids = roles.stream().map(Role::getRoleId).toList();
        roleRepository.findWithReportTypesByIds(ids);
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> found = permissionRepository.findAllById(permissionIds);
        if (found.size() != permissionIds.size()) {
            throw new BusinessException("One or more permissions were not found");
        }
        return new HashSet<>(found);
    }

    /**
     * Résout les types. Les nouvelles affectations doivent être actives ;
     * un type déjà lié au rôle peut rester même s'il a été désactivé depuis.
     */
    private Set<ReportType> resolveReportTypes(Set<Long> typeIds, Set<Long> previouslyAssigned) {
        if (typeIds == null || typeIds.isEmpty()) {
            return new HashSet<>();
        }
        List<ReportType> found = reportTypeRepository.findAllById(typeIds);
        if (found.size() != typeIds.size()) {
            throw new BusinessException("One or more report types were not found");
        }
        for (ReportType type : found) {
            boolean alreadyAssigned = previouslyAssigned.contains(type.getReportTypeId());
            if (!type.isActive() && !alreadyAssigned) {
                throw new BusinessException(
                        "Cannot assign inactive report type: " + type.getCode(),
                        "REPORT_TYPE_INACTIVE");
            }
        }
        return new HashSet<>(found);
    }
}
