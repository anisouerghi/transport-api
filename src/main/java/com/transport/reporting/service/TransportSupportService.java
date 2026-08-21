package com.transport.reporting.service;

import com.transport.reporting.common.dto.SearchRequest;
import com.transport.reporting.common.enums.AuditAction;
import com.transport.reporting.common.enums.AuditModule;
import com.transport.reporting.common.enums.QrStatus;
import com.transport.reporting.common.enums.SupportStatus;
import com.transport.reporting.common.response.PageResponse;
import com.transport.reporting.common.util.AuditActors;
import com.transport.reporting.common.util.PageableUtils;
import com.transport.reporting.dto.AuditLogEvent;
import com.transport.reporting.dto.PublicSupportOptionResponse;
import com.transport.reporting.dto.TransportSupportCriteria;
import com.transport.reporting.dto.TransportSupportRequest;
import com.transport.reporting.dto.TransportSupportResponse;
import com.transport.reporting.entity.SupportType;
import com.transport.reporting.entity.TransportSupport;
import com.transport.reporting.entity.District;
import com.transport.reporting.exception.BusinessException;
import com.transport.reporting.exception.ResourceNotFoundException;
import com.transport.reporting.mapper.TransportSupportMapper;
import com.transport.reporting.repository.ReportRepository;
import com.transport.reporting.repository.SupportTypeRepository;
import com.transport.reporting.repository.DistrictRepository;
import com.transport.reporting.repository.TransportSupportRepository;
import com.transport.reporting.specification.TransportSupportSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service metier TransportSupport (CRUD + recherche + generation QR).
 * <p>
 * A la creation :
 * <ol>
 *   <li>Persistance initiale (UUID genere dans @PrePersist)</li>
 *   <li>Construction URL publique + generation image QR</li>
 *   <li>Mise a jour qrCodeUrl / qrCodePath</li>
 * </ol>
 */
@Service
@Transactional
public class TransportSupportService {

    /** Mapping nom logique frontend -> attribut JPA pour le tri. */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "transportSupportId",
            "reference", "reference",
            "label", "label",
            "qrDateCreation", "qrDateCreation",
            "qrDateImpression", "qrDateImpression",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "supportStatus", "supportStatus",
            "qrStatus", "qrStatus"
    );

    private final TransportSupportRepository transportSupportRepository;
    private final SupportTypeRepository supportTypeRepository;
    private final DistrictRepository districtRepository;
    private final TransportSupportMapper transportSupportMapper;
    private final QrCodeService qrCodeService;
    private final AuditLogService auditLogService;
    private final ReportRepository reportRepository;
    public TransportSupportService(TransportSupportRepository transportSupportRepository, SupportTypeRepository supportTypeRepository, DistrictRepository districtRepository, TransportSupportMapper transportSupportMapper, QrCodeService qrCodeService, AuditLogService auditLogService, ReportRepository reportRepository) {
        this.transportSupportRepository = transportSupportRepository;
        this.supportTypeRepository = supportTypeRepository;
        this.districtRepository = districtRepository;
        this.transportSupportMapper = transportSupportMapper;
        this.qrCodeService = qrCodeService;
        this.auditLogService = auditLogService;
        this.reportRepository = reportRepository;
    }


    /**
     * Consultation publique par UUID : uniquement si le support est ACTIVE.
     * Utilise par le scan QR (API publique).
     */
    @Transactional(readOnly = true)
    public TransportSupportResponse findActiveByUuid(UUID uuid) {
        TransportSupport support = transportSupportRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("TransportSupport", uuid));
        if (support.getSupportStatus() != SupportStatus.ACTIVE) {
            throw new ResourceNotFoundException("Active TransportSupport", uuid);
        }
        return transportSupportMapper.toResponse(support);
    }

    /** Catalogue public : supports actifs pour le choix sans QR. */
    @Transactional(readOnly = true)
    public List<PublicSupportOptionResponse> findAllActivePublic() {
        return transportSupportRepository.findBySupportStatusWithType(SupportStatus.ACTIVE).stream()
                .map(s -> PublicSupportOptionResponse.builder()
                        .uuid(s.getUuid())
                        .reference(s.getReference())
                        .label(s.getLabel())
                        .supportTypeId(s.getSupportType() != null ? s.getSupportType().getSupportTypeId() : null)
                        .supportTypeCode(s.getSupportType() != null ? s.getSupportType().getCode() : null)
                        .supportTypeLabel(s.getSupportType() != null ? s.getSupportType().getLabel() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /** Liste complete sans pagination. */
    @Transactional(readOnly = true)
    public List<TransportSupportResponse> findAll() {
        return transportSupportRepository.findAll().stream()
                .map(transportSupportMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Consultation detaillee par id technique. */
    @Transactional(readOnly = true)
    public TransportSupportResponse findById(Long id) {
        return transportSupportMapper.toResponse(getEntity(id));
    }

    /** Recherche paginee multicritere (POST /search). */
    @Transactional(readOnly = true)
    public PageResponse<TransportSupportResponse> search(SearchRequest<TransportSupportCriteria> request) {
        TransportSupportCriteria criteria = request.getFilters();
        Pageable pageable = PageableUtils.toPageable(request.getPageable(), "transportSupportId", SORT_FIELDS);
        Specification<TransportSupport> spec = TransportSupportSpecification.fromCriteria(criteria);
        Page<TransportSupportResponse> page = transportSupportRepository.findAll(spec, pageable)
                .map(transportSupportMapper::toResponse);
        return PageResponse.from(page);
    }

    /**
     * Creation d'un support + generation automatique du QR Code.
     * Le frontend n'envoie que reference, label, supportTypeId, supportStatus.
     */
    public TransportSupportResponse create(TransportSupportRequest request) {
        if (transportSupportRepository.existsByReference(request.getReference())) {
            throw new BusinessException("Support reference already exists");
        }
        SupportType supportType = resolveSupportType(request.getSupportTypeId());
        District district = resolveDistrict(request.getDistrictId());

        TransportSupport support = transportSupportMapper.toEntity(request, supportType, district);
        support.setSupportStatus(request.getSupportStatus() != null
                ? request.getSupportStatus()
                : SupportStatus.ACTIVE);

        // flush pour obtenir l'UUID genere par @PrePersist avant de construire l'URL QR
        support = transportSupportRepository.saveAndFlush(support);
        applyQrCode(support);
        support = transportSupportRepository.save(support);

        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.CREATE)
                .module(AuditModule.TRANSPORT_SUPPORTS)
                .entityName("TransportSupport")
                .entityId(String.valueOf(support.getTransportSupportId()))
                .newValue(snapshot(support))
                .description("Création du support " + support.getReference())
                .build());

        return transportSupportMapper.toResponse(support);
    }

    /**
     * Modification des champs metier uniquement (pas de regeneration QR automatique).
     * Utiliser {@link #regenerateQr(Long)} pour regenerer le QR.
     */
    public TransportSupportResponse update(Long id, TransportSupportRequest request) {
        TransportSupport support = getEntity(id);
        String oldValue = snapshot(support);

        if (!support.getReference().equals(request.getReference())
                && transportSupportRepository.existsByReferenceAndTransportSupportIdNot(
                request.getReference(), id)) {
            throw new BusinessException("Support reference already exists");
        }

        SupportType supportType = resolveSupportType(request.getSupportTypeId());
        District district = resolveDistrict(request.getDistrictId());
        transportSupportMapper.updateEntity(support, request, supportType, district);
        support = transportSupportRepository.save(support);

        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.TRANSPORT_SUPPORTS)
                .entityName("TransportSupport")
                .entityId(String.valueOf(support.getTransportSupportId()))
                .oldValue(oldValue)
                .newValue(snapshot(support))
                .description("Modification du support " + support.getReference())
                .build());

        return transportSupportMapper.toResponse(support);
    }

    /** Regenere l'image QR et remet qrStatus a GENERATED. */
    public TransportSupportResponse regenerateQr(Long id) {
        TransportSupport support = getEntity(id);
        
        // Verifier si le transport_support_id possede deja un rapport
        if (reportRepository.existsByTransportSupportTransportSupportId(support.getTransportSupportId())) {
            throw new BusinessException("Cannot regenerate QR code: this transport support already has a report");
        }
        
        applyQrCode(support);
        support.setQrDateCreation(Instant.now());
        support.setQrStatus(QrStatus.GENERATED);
        support = transportSupportRepository.save(support);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.TRANSPORT_SUPPORTS)
                .entityName("TransportSupport")
                .entityId(String.valueOf(support.getTransportSupportId()))
                .newValue("qrStatus=" + support.getQrStatus() + ";qrCodeUrl=" + support.getQrCodeUrl())
                .description("Régénération du QR du support " + support.getReference())
                .build());
        return transportSupportMapper.toResponse(support);
    }

    /** Regenerer l'image QR pour tous les supports et mettre a jour leur statut. */
    public List<TransportSupportResponse> regenerateQrAll() {
        List<TransportSupport> supports = transportSupportRepository.findAll();
        Instant now = Instant.now();
        int regeneratedCount = 0;
        
        for (TransportSupport support : supports) {
            // Sauter la regeneration si le transport_support_id possede deja un rapport
            if (reportRepository.existsByTransportSupportTransportSupportId(support.getTransportSupportId())) {
                continue;
            }
            
            applyQrCode(support);
            support.setQrDateCreation(now);
            support.setQrStatus(QrStatus.GENERATED);
            regeneratedCount++;
        }
        
        transportSupportRepository.saveAll(supports);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.UPDATE)
                .module(AuditModule.TRANSPORT_SUPPORTS)
                .entityName("TransportSupport")
                .entityId("ALL")
                .newValue("qrCount=" + regeneratedCount + ";totalCount=" + supports.size())
                .description("Régénération des QR Codes de tous les supports (" + regeneratedCount + "/" + supports.size() + ")")
                .build());
        return supports.stream()
                .map(transportSupportMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Retourne les octets de l'image PNG du QR Code. */
    @Transactional(readOnly = true)
    public byte[] getQrImage(Long id) {
        TransportSupport support = getEntity(id);
        return qrCodeService.readQrImage(support);
    }

    /** Suppression physique du support. */
    public void delete(Long id) {
        TransportSupport support = getEntity(id);
        String oldValue = snapshot(support);
        transportSupportRepository.deleteById(id);
        auditLogService.record(AuditLogEvent.builder()
                .userId(AuditActors.currentAdminUserId())
                .actionType(AuditAction.DELETE)
                .module(AuditModule.TRANSPORT_SUPPORTS)
                .entityName("TransportSupport")
                .entityId(String.valueOf(id))
                .oldValue(oldValue)
                .description("Suppression du support " + support.getReference())
                .build());
    }

    private static String snapshot(TransportSupport support) {
        return "reference=" + support.getReference()
                + ";label=" + support.getLabel()
                + ";supportStatus=" + support.getSupportStatus()
                + ";qrStatus=" + support.getQrStatus();
    }

    /**
     * Construit l'URL publique et genere le fichier image,
     * puis renseigne qrCodeUrl et qrCodePath sur l'entite.
     */
    private void applyQrCode(TransportSupport support) {
        String publicUrl = qrCodeService.buildPublicUrl(support);
        String qrPath = qrCodeService.generateAndStore(support);
        support.setQrCodeUrl(publicUrl);
        support.setQrCodePath(qrPath);
        if (support.getQrStatus() == null) {
            support.setQrStatus(QrStatus.GENERATED);
        }
    }

    private SupportType resolveSupportType(Long supportTypeId) {
        return supportTypeRepository.findById(supportTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportType", supportTypeId));
    }

    private District resolveDistrict(Long districtId) {
        return districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", districtId));
    }

    private TransportSupport getEntity(Long id) {
        return transportSupportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TransportSupport", id));
    }
}
