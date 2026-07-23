package com.transport.reporting.modules.signalement.service;

import com.transport.reporting.common.enums.StatutSignalement;
import com.transport.reporting.common.exception.BusinessException;
import com.transport.reporting.common.exception.ResourceNotFoundException;
import com.transport.reporting.common.utils.ReferenceUtils;
import com.transport.reporting.modules.signalement.dto.*;
import com.transport.reporting.modules.signalement.entity.Signalement;
import com.transport.reporting.modules.signalement.mapper.SignalementMapper;
import com.transport.reporting.modules.signalement.repository.SignalementRepository;
import com.transport.reporting.modules.support.entity.Support;
import com.transport.reporting.modules.support.repository.SupportRepository;
import com.transport.reporting.modules.voyageur.entity.Voyageur;
import com.transport.reporting.modules.voyageur.service.VoyageurService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SignalementServiceImpl implements SignalementService {

    private final SignalementRepository signalementRepository;
    private final SupportRepository supportRepository;
    private final VoyageurService voyageurService;
    private final SignalementMapper signalementMapper;

    @Override
    public SignalementResponse create(SignalementRequest request) {
        Support support = supportRepository.findByUuidAndActifTrue(request.getSupportUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Support actif", request.getSupportUuid()));

        Voyageur voyageur = voyageurService.findOrCreate(request.getVoyageur());

        Signalement signalement = Signalement.builder()
                .reference(generateUniqueReference())
                .type(request.getType())
                .statut(StatutSignalement.NOUVEAU)
                .objet(request.getObjet())
                .description(request.getDescription())
                .support(support)
                .voyageur(voyageur)
                .build();

        return signalementMapper.toResponse(signalementRepository.save(signalement));
    }

    @Override
    @Transactional(readOnly = true)
    public SignalementResponse getByReference(String reference) {
        return signalementMapper.toResponse(findByReference(reference));
    }

    @Override
    @Transactional(readOnly = true)
    public SignalementResponse getById(Long id) {
        return signalementMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignalementResponse> search(SignalementSearchDTO criteria, Pageable pageable) {
        return signalementRepository.findAll(buildSpecification(criteria), pageable)
                .map(signalementMapper::toResponse);
    }

    @Override
    public SignalementResponse changeStatut(Long id, ChangeStatutRequest request) {
        Signalement signalement = findEntity(id);
        if (signalement.getStatut() == request.getStatut()) {
            throw new BusinessException("Le signalement est déjà au statut " + request.getStatut());
        }
        signalement.setStatut(request.getStatut());
        return signalementMapper.toResponse(signalementRepository.save(signalement));
    }

    @Override
    public SignalementResponse affecter(Long id, AffectationRequest request) {
        Signalement signalement = findEntity(id);
        signalement.setServiceAffecte(request.getServiceAffecte());
        if (signalement.getStatut() == StatutSignalement.NOUVEAU) {
            signalement.setStatut(StatutSignalement.EN_COURS);
        }
        return signalementMapper.toResponse(signalementRepository.save(signalement));
    }

    @Override
    public SignalementResponse repondre(Long id, ReponseRequest request) {
        Signalement signalement = findEntity(id);
        signalement.setReponse(request.getReponse());
        return signalementMapper.toResponse(signalementRepository.save(signalement));
    }

    private String generateUniqueReference() {
        String reference;
        do {
            reference = ReferenceUtils.generateSignalementReference();
        } while (signalementRepository.existsByReference(reference));
        return reference;
    }

    private Signalement findEntity(Long id) {
        return signalementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement", id));
    }

    private Signalement findByReference(String reference) {
        return signalementRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement", reference));
    }

    private Specification<Signalement> buildSpecification(SignalementSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria == null) {
                return cb.conjunction();
            }
            if (StringUtils.hasText(criteria.getReference())) {
                predicates.add(cb.equal(root.get("reference"), criteria.getReference()));
            }
            if (criteria.getType() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.getType()));
            }
            if (criteria.getStatut() != null) {
                predicates.add(cb.equal(root.get("statut"), criteria.getStatut()));
            }
            if (StringUtils.hasText(criteria.getServiceAffecte())) {
                predicates.add(cb.equal(root.get("serviceAffecte"), criteria.getServiceAffecte()));
            }
            if (criteria.getSupportId() != null) {
                predicates.add(cb.equal(root.get("support").get("id"), criteria.getSupportId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
