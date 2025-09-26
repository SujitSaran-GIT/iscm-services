package com.iscm.iam.repository;

import com.iscm.iam.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findByUserIdAndAttemptTimeAfterAndSuccessfulFalse(UUID userId, LocalDateTime since);

    List<LoginAttempt> findByUserIdAndSuccessfulTrueOrderByAttemptTimeDesc(UUID userId);

    List<LoginAttempt> findByUserIdAndIpAddressAndSuccessfulFalseOrderByAttemptTimeDesc(UUID userId, String ipAddress);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.userId = :userId AND la.attemptTime > :since")
    long countByUserIdAndAttemptTimeAfter(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.userId = :userId AND la.attemptTime > :since AND la.successful = false")
    long countByUserIdAndAttemptTimeAfterAndSuccessfulFalse(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    List<LoginAttempt> findByUserIdAndIpAddress(UUID userId, String ipAddress);

    void deleteAllByAttemptTimeBefore(LocalDateTime dateTime);
}