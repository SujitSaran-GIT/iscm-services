package com.iscm.iam.repository;

import com.iscm.iam.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint);

    List<Device> findByUserIdOrderByLastSeenAtDesc(UUID userId);

    List<Device> findByUserIdAndIsTrustedTrue(UUID userId);

    List<Device> findByUserIdAndLastSeenIp(UUID userId, String ipAddress);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.user.id = :userId AND d.isTrusted = true")
    long countByUserIdAndIsTrustedTrue(@Param("userId") UUID userId);

    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.lastSeenAt < :cutoffDate")
    List<Device> findExpiredDevices(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);

    long countByUserId(UUID userId);

    void deleteAllByLastSeenAtBefore(LocalDateTime dateTime);
}