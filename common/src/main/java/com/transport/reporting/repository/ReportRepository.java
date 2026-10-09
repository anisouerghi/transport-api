package com.transport.reporting.repository;

import com.transport.reporting.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA des signalements.
 */
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    Optional<Report> findByReference(String reference);

    Optional<Report> findByUuid(UUID uuid);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.passenger LEFT JOIN FETCH r.status WHERE r.reportId = :id")
    Optional<Report> findByIdWithPassenger(@Param("id") Long id);

    /**
     * Nombre de signalements par type, du plus fréquent au moins fréquent.
     * Chaque ligne est un couple {@code [ReportType, Long count]}.
     * Les signalements sans type renseigné sont ignorés.
     */
    @Query("""
            SELECT r.reportType, COUNT(r)
            FROM Report r
            WHERE r.reportType IS NOT NULL
            GROUP BY r.reportType
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countReportsGroupedByType();

    /**
     * Nombre de signalements par statut, du plus fréquent au moins fréquent.
     * Chaque ligne est un couple {@code [Status, Long count]}.
     * Les signalements sans statut renseigné sont ignorés.
     */
    @Query("""
            SELECT r.status, COUNT(r)
            FROM Report r
            WHERE r.status IS NOT NULL
            GROUP BY r.status
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countReportsGroupedByStatus();

    /**
     * Nombre de signalements par type de support, du plus fréquent au moins fréquent.
     * Le type est atteint via le support de transport du signalement.
     * Chaque ligne est un couple {@code [SupportType, Long count]} ; le type vaut
     * {@code null} pour le groupe des signalements rattachés à aucun support
     * (bucket « Sans support »).
     */
    @Query("""
            SELECT st, COUNT(r)
            FROM Report r
            LEFT JOIN r.transportSupport ts
            LEFT JOIN ts.supportType st
            GROUP BY st
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countReportsGroupedBySupportType();

    /**
     * Signalements authentifiés : voyageur rattaché ayant validé son adresse e-mail.
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.passenger IS NOT NULL AND r.passenger.emailVerified = true")
    long countAuthenticatedReports();

    /**
     * Signalements anonymes : sans voyageur rattaché ou voyageur dont l'adresse
     * e-mail n'a pas été validée.
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.passenger IS NULL OR r.passenger.emailVerified = false")
    long countAnonymousReports();

    boolean existsByReference(String reference);


    boolean existsByTransportSupportTransportSupportId(Long transportSupportId);

    boolean existsByNature_ReportNatureId(Long reportNatureId);

}
