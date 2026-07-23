package com.transport.reporting.modules.utilisateur.service;

import com.transport.reporting.modules.utilisateur.dto.UtilisateurRequest;
import com.transport.reporting.modules.utilisateur.dto.UtilisateurResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UtilisateurService {

    UtilisateurResponse create(UtilisateurRequest request);

    UtilisateurResponse getById(Long id);

    Page<UtilisateurResponse> findAll(Pageable pageable);
}
