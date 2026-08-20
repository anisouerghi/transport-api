package com.transport.reporting.service;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.UpdatePasswordRequest;
import com.transport.reporting.dto.UserRequest;
import com.transport.reporting.dto.UserResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.entity.Role;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.UserMapper;
import com.transport.reporting.repository.RoleRepository;
import com.transport.reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service metier Utilisateur (CRUD + rôles dynamiques).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllWithRoles().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getEntityWithRoles(id));
    }

    public UserResponse create(UserRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("Password is required");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        AppUser user = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        user.setRoles(resolveRoles(request.getRoleIds()));
        user = userRepository.save(user);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.CREATE)
                .module(AuditModule.USERS)
                .entityName("AppUser")
                .entityId(String.valueOf(user.getUserId()))
                .newValue(snapshot(user))
                .description("Création de l'utilisateur " + user.getUsername())
                .build());
        return userMapper.toResponse(getEntityWithRoles(user.getUserId()));
    }

    public UserResponse update(Long id, UserRequest request) {
        AppUser user = getEntityWithRoles(id);
        String oldValue = snapshot(user);

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Email already exists");
        }

        String passwordHash = StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword())
                : null;
        userMapper.updateEntity(user, request, passwordHash);
        user.setRoles(resolveRoles(request.getRoleIds()));
        user = userRepository.save(user);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.USERS)
                .entityName("AppUser")
                .entityId(String.valueOf(user.getUserId()))
                .oldValue(oldValue)
                .newValue(snapshot(user))
                .description("Modification de l'utilisateur " + user.getUsername())
                .build());
        return userMapper.toResponse(getEntityWithRoles(user.getUserId()));
    }

    public UserResponse setActive(Long id, boolean active) {
        AppUser user = getEntityWithRoles(id);
        String oldValue = "active=" + user.isActive();
        user.setActive(active);
        user = userRepository.save(user);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.USERS)
                .entityName("AppUser")
                .entityId(String.valueOf(user.getUserId()))
                .oldValue(oldValue)
                .newValue("active=" + user.isActive())
                .description((active ? "Activation" : "Désactivation") + " de l'utilisateur " + user.getUsername())
                .build());
        return userMapper.toResponse(user);
    }

    public void delete(Long id) {
        AppUser user = getEntityWithRoles(id);
        String snapshot = snapshot(user);
        userRepository.deleteById(id);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.DELETE)
                .module(AuditModule.USERS)
                .entityName("AppUser")
                .entityId(String.valueOf(id))
                .oldValue(snapshot)
                .description("Suppression de l'utilisateur " + user.getUsername())
                .build());
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Ancien mot de passe incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.record(AuditLogEvent.builder()
                .userId(userId)
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.USERS)
                .entityName("AppUser")
                .entityId(String.valueOf(userId))
                .description("Mise à jour du mot de passe de l'utilisateur " + user.getUsername())
                .build());
    }

    private AppUser getEntityWithRoles(Long id) {
        return userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) {
            throw new BusinessException("One or more roles were not found");
        }
        return new HashSet<>(found);
    }

    private static String snapshot(AppUser user) {
        String roles = user.getRoles() == null ? ""
                : user.getRoles().stream().map(Role::getCode).sorted().reduce((a, b) -> a + "," + b).orElse("");
        return "username=" + user.getUsername()
                + ";name=" + user.getName()
                + ";email=" + user.getEmail()
                + ";active=" + user.isActive()
                + ";roles=" + roles;
    }
}
