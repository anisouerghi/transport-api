package com.transport.reporting.service;

import com.transport.reporting.dto.PassengerNotificationInboxResponse;
import com.transport.reporting.dto.PassengerNotificationItemResponse;
import com.transport.reporting.dto.PassengerNotificationReadRequest;
import com.transport.reporting.entity.Passenger;
import com.transport.reporting.entity.PassengerNotification;
import com.transport.reporting.entity.Report;
import com.transport.reporting.repository.PassengerNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Création et consultation des notifications plateforme voyageur.
 * La création est transactionnelle avec le traitement agent ; le polling ne crée rien.
 */
@Service
public class PassengerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PassengerNotificationService.class);

    private static final int INBOX_LIMIT = 20;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(Locale.FRANCE)
            .withZone(ZoneId.of("Africa/Tunis"));

    private final PassengerNotificationRepository notificationRepository;
    private final PassengerPlatformNotificationGuard platformGuard;
    private final PublicStatusMapper publicStatusMapper;

    public PassengerNotificationService(
            PassengerNotificationRepository notificationRepository,
            PassengerPlatformNotificationGuard platformGuard,
            PublicStatusMapper publicStatusMapper) {
        this.notificationRepository = notificationRepository;
        this.platformGuard = platformGuard;
        this.publicStatusMapper = publicStatusMapper;
    }

    /**
     * Crée une notification si le voyageur a un compte suivi, {@code notify_platform}
     * autorisé, et qu'aucune ligne n'existe déjà pour la clé anti-doublon.
     *
     * @return true si une ligne a été créée
     */
    @Transactional
    public boolean createForStatusOrComplement(
            Report report, String eventType, String publicStatusLabel, Long replyId) {
        if (report == null) {
            return false;
        }
        Passenger passenger = report.getPassenger();
        if (passenger == null || !passenger.hasTrackedAccount()) {
            log.debug("Notif plateforme ignorée (voyageur anonyme) reportId={}", report.getReportId());
            return false;
        }
        if (platformGuard.evaluate(passenger) != PassengerPlatformNotificationGuard.Decision.ALLOW) {
            log.info(
                    "Notif plateforme ignorée (préférence) reportId={} passengerId={}",
                    report.getReportId(),
                    passenger.getPassengerId());
            return false;
        }
        if (!StringUtils.hasText(eventType)) {
            return false;
        }

        String publicStatus = StringUtils.hasText(publicStatusLabel) ? publicStatusLabel.trim() : null;
        String dedupKey = buildDedupKey(eventType, report.getReportId(), publicStatus, replyId);
        if (!StringUtils.hasText(dedupKey)) {
            return false;
        }
        if (notificationRepository.existsByPassenger_PassengerIdAndDedupKey(
                passenger.getPassengerId(), dedupKey)) {
            log.debug("Notif plateforme déjà présente dedupKey={}", dedupKey);
            return false;
        }

        String message = buildMessage(report.getReference(), publicStatus, Instant.now());
        PassengerNotification notification = PassengerNotification.builder()
                .passenger(passenger)
                .report(report)
                .eventType(eventType)
                .publicStatus(publicStatus)
                .message(message)
                .dedupKey(dedupKey)
                .build();
        try {
            notificationRepository.save(notification);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Course concurrente : même opération métier, une seule notif suffit.
            log.debug("Notif plateforme doublon concurrent ignoré dedupKey={}", dedupKey);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PassengerNotificationInboxResponse inbox(Long passengerId) {
        if (passengerId == null) {
            return PassengerNotificationInboxResponse.builder()
                    .unreadCount(0)
                    .items(List.of())
                    .build();
        }
        long unread = notificationRepository.countByPassenger_PassengerIdAndReadAtIsNull(passengerId);
        List<PassengerNotificationItemResponse> items = notificationRepository
                .findByPassenger_PassengerIdOrderByCreatedAtDescNotificationIdDesc(
                        passengerId, PageRequest.of(0, INBOX_LIMIT))
                .stream()
                .map(this::toItem)
                .toList();
        return PassengerNotificationInboxResponse.builder()
                .unreadCount(unread)
                .items(items)
                .build();
    }

    @Transactional
    public int markRead(Long passengerId, PassengerNotificationReadRequest request) {
        if (passengerId == null) {
            return 0;
        }
        boolean markAll = request != null && request.isMarkAll();
        List<Long> ids = request != null && request.getIds() != null
                ? request.getIds()
                : Collections.emptyList();
        if (!markAll && ids.isEmpty()) {
            return 0;
        }
        // JPQL IN () vide interdit : passer une liste factice si markAll.
        CollectionOrPlaceholder holder = markAll
                ? CollectionOrPlaceholder.all()
                : CollectionOrPlaceholder.of(ids);
        return notificationRepository.markRead(
                passengerId, Instant.now(), holder.markAll(), holder.ids());
    }

    static String buildDedupKey(String eventType, Long reportId, String publicStatus, Long replyId) {
        if (PassengerNotification.EVENT_COMPLEMENT_REQUEST.equals(eventType)) {
            if (replyId == null) {
                return null;
            }
            return "COMPLEMENT:reply:" + replyId;
        }
        if (PassengerNotification.EVENT_STATUS_PUBLIC_CHANGED.equals(eventType)) {
            if (reportId == null || !StringUtils.hasText(publicStatus)) {
                return null;
            }
            return "STATUS:" + reportId + ":" + publicStatus.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private String buildMessage(String reference, String publicStatus, Instant at) {
        String ref = StringUtils.hasText(reference) ? reference.trim() : "—";
        String when = DISPLAY_DATE.format(at);
        if (StringUtils.hasText(publicStatus)) {
            return "Signalement " + ref + " — " + publicStatus.trim() + " (" + when + ")";
        }
        return publicStatusMapper.defaultNotificationMessage(null)
                + " Réf. " + ref + " (" + when + ")";
    }

    private PassengerNotificationItemResponse toItem(PassengerNotification n) {
        Report report = n.getReport();
        return PassengerNotificationItemResponse.builder()
                .id(n.getNotificationId())
                .reportReference(report != null ? report.getReference() : null)
                .reportUuid(report != null ? report.getUuid() : null)
                .message(n.getMessage())
                .publicStatus(n.getPublicStatus())
                .eventType(n.getEventType())
                .createdAt(n.getCreatedAt())
                .read(n.isRead())
                .build();
    }

    /** Évite un {@code IN ()} vide en JPQL lorsque markAll=true. */
    private record CollectionOrPlaceholder(boolean markAll, List<Long> ids) {
        static CollectionOrPlaceholder all() {
            return new CollectionOrPlaceholder(true, List.of(-1L));
        }

        static CollectionOrPlaceholder of(List<Long> ids) {
            return new CollectionOrPlaceholder(false, ids);
        }
    }
}
