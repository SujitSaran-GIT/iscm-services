package com.iscm.iam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_sessions", 
       indexes = {
           @Index(name = "idx_user_sessions_user", columnList = "user_id"),
           @Index(name = "idx_user_sessions_expires", columnList = "expires_at")
       })
public class UserSession extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    private Boolean revoked = false;
}