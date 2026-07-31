package com.transport.reporting.service;

import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.UserRequest;
import com.transport.reporting.dto.UserResponse;
import com.transport.reporting.entity.AppUser;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.UserMapper;
import com.transport.reporting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service metier Utilisateur (CRUD complet - modele de reference).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getEntity(id));
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
        return userMapper.toResponse(user);
    }

    public UserResponse update(Long id, UserRequest request) {
        AppUser user = getEntity(id);
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
        return userMapper.toResponse(user);
    }

    public UserResponse setActive(Long id, boolean active) {
        AppUser user = getEntity(id);
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
        AppUser user = getEntity(id);
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

    private AppUser getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static String snapshot(AppUser user) {
        return "username=" + user.getUsername()
                + ";name=" + user.getName()
                + ";email=" + user.getEmail()
                + ";active=" + user.isActive();
    }
}
