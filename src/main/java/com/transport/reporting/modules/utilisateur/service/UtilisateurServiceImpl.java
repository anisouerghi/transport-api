package com.transport.reporting.modules.utilisateur.service;

import com.transport.reporting.common.exception.BusinessException;
import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.modules.utilisateur.dto.UtilisateurRequest;
import com.transport.reporting.modules.utilisateur.dto.UtilisateurResponse;
import com.transport.reporting.modules.utilisateur.entity.Utilisateur;
import com.transport.reporting.modules.utilisateur.mapper.UtilisateurMapper;
import com.transport.reporting.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UtilisateurResponse create(UtilisateurRequest request) {
        if (utilisateurRepository.existsByLogin(request.getLogin())) {
            throw new BusinessException("Le login existe déjà");
        }
        if (utilisateurRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("L'email existe déjà");
        }
        Utilisateur utilisateur = utilisateurMapper.toEntity(
                request,
                passwordEncoder.encode(request.getPassword())
        );
        return utilisateurMapper.toResponse(utilisateurRepository.save(utilisateur));
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponse getById(Long id) {
        return utilisateurMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilisateurResponse> findAll(Pageable pageable) {
        return utilisateurRepository.findAll(pageable).map(utilisateurMapper::toResponse);
    }

    private Utilisateur findEntity(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }
}
