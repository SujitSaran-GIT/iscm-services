package com.iscm.iam.repository;

import com.iscm.iam.model.SuspiciousActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, UUID> {

    List<SuspiciousActivity> findByUserIdAndActivityTypeAndTimestampAfter(UUID userId, String activityType, LocalDateTime since);

    List<SuspiciousActivity> findByUserIdAndTimestampAfterOrderByTimestampDesc(UUID userId, LocalDateTime since);

    @Query("SELECT COUNT(sa) FROM SuspiciousActivity sa WHERE sa.userId = :userId AND sa.timestamp > :since")
    long countByUserIdAndTimestampAfter(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    void deleteAllByTimestampBefore(LocalDateTime dateTime);
}