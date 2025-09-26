package com.iscm.iam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "login_attempts",
       indexes = {
           @Index(name = "idx_login_attempts_user", columnList = "user_id"),
           @Index(name = "idx_login_attempts_time", columnList = "attempt_time"),
           @Index(name = "idx_login_attempts_ip", columnList = "ip_address")
       })
public class LoginAttempt extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "successful", nullable = false)
    private Boolean successful;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(name = "failure_reason")
    private String failureReason;
}