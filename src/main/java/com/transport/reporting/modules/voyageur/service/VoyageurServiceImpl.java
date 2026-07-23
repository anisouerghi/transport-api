package com.transport.reporting.modules.voyageur.service;

import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.modules.voyageur.dto.VoyageurRequest;
import com.transport.reporting.modules.voyageur.dto.VoyageurResponse;
import com.transport.reporting.modules.voyageur.entity.Voyageur;
import com.transport.reporting.modules.voyageur.mapper.VoyageurMapper;
import com.transport.reporting.modules.voyageur.repository.VoyageurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class VoyageurServiceImpl implements VoyageurService {

    private final VoyageurRepository voyageurRepository;
    private final VoyageurMapper voyageurMapper;

    @Override
    public Voyageur findOrCreate(VoyageurRequest request) {
        if (StringUtils.hasText(request.getEmail())) {
            return voyageurRepository.findByEmailIgnoreCase(request.getEmail())
                    .map(existing -> updateIfNeeded(existing, request))
                    .orElseGet(() -> voyageurRepository.save(voyageurMapper.toEntity(request)));
        }
        return voyageurRepository.save(voyageurMapper.toEntity(request));
    }

    @Override
    @Transactional(readOnly = true)
    public VoyageurResponse getById(Long id) {
        Voyageur voyageur = voyageurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyageur", id));
        return voyageurMapper.toResponse(voyageur);
    }

    private Voyageur updateIfNeeded(Voyageur existing, VoyageurRequest request) {
        if (request.getNom() != null) {
            existing.setNom(request.getNom());
        }
        if (request.getTelephone() != null) {
            existing.setTelephone(request.getTelephone());
        }
        return voyageurRepository.save(existing);
    }
}
