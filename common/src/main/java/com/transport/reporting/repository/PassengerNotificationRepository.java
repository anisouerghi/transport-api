package com.transport.reporting.repository;

import com.transport.reporting.entity.PassengerNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PassengerNotificationRepository extends JpaRepository<PassengerNotification, Long> {

    boolean existsByPassenger_PassengerIdAndDedupKey(Long passengerId, String dedupKey);

    long countByPassenger_PassengerIdAndReadAtIsNull(Long passengerId);

    List<PassengerNotification> findByPassenger_PassengerIdOrderByCreatedAtDescNotificationIdDesc(
            Long passengerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PassengerNotification n
            SET n.readAt = :readAt
            WHERE n.passenger.passengerId = :passengerId
              AND n.readAt IS NULL
              AND (:markAll = true OR n.notificationId IN :ids)
            """)
    int markRead(
            @Param("passengerId") Long passengerId,
            @Param("readAt") Instant readAt,
            @Param("markAll") boolean markAll,
            @Param("ids") Collection<Long> ids);
}
