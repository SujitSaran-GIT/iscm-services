package com.iscm.iam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "suspicious_activities",
       indexes = {
           @Index(name = "idx_suspicious_activities_user", columnList = "user_id"),
           @Index(name = "idx_suspicious_activities_time", columnList = "timestamp"),
           @Index(name = "idx_suspicious_activities_type", columnList = "activity_type")
       })
public class SuspiciousActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    @Column(name = "description")
    private String description;

    @Column(name = "investigated")
    private Boolean investigated = false;

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}