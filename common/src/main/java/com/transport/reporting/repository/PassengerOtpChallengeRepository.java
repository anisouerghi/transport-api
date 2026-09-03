package com.transport.reporting.repository;

import com.transport.reporting.entity.OtpChallengeStatus;
import com.transport.reporting.entity.PassengerOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PassengerOtpChallengeRepository extends JpaRepository<PassengerOtpChallenge, Long> {

    Optional<PassengerOtpChallenge> findByTransactionId(String transactionId);

    @Modifying
    @Query("UPDATE PassengerOtpChallenge c SET c.status = :cancelled "
            + "WHERE c.passengerId = :passengerId AND c.status = :pending")
    int cancelPendingForPassenger(
            @Param("passengerId") Long passengerId,
            @Param("pending") OtpChallengeStatus pending,
            @Param("cancelled") OtpChallengeStatus cancelled);
}
