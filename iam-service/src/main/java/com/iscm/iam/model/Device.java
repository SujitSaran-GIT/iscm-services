package com.iscm.iam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_devices",
       indexes = {
           @Index(name = "idx_user_devices_user", columnList = "user_id"),
           @Index(name = "idx_user_devices_fingerprint", columnList = "device_fingerprint")
       })
public class Device extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_type") // mobile, desktop, tablet
    private String deviceType;

    @Column(name = "operating_system")
    private String operatingSystem;

    @Column(name = "browser")
    private String browser;

    @Column(name = "last_seen_ip")
    private String lastSeenIp;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "is_trusted")
    private Boolean isTrusted = false;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "trust_score")
    private Integer trustScore = 50; // 0-100 scale

    public void updateLastSeen(String ipAddress) {
        this.lastSeenIp = ipAddress;
        this.lastSeenAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return lastSeenAt.isBefore(LocalDateTime.now().minusDays(90));
    }
}