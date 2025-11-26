package com.iscm.iam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "oauth_accounts",
       indexes = {
           @Index(name = "idx_oauth_accounts_user", columnList = "user_id"),
           @Index(name = "idx_oauth_accounts_provider", columnList = "provider"),
           @Index(name = "idx_oauth_accounts_provider_id", columnList = "provider_id")
       })
public class OAuthAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "provider_username")
    private String providerUsername;

    @Column(name = "email")
    private String email;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "scopes")
    private String scopes;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public boolean isTokenExpired() {
        return tokenExpiry != null && tokenExpiry.isBefore(LocalDateTime.now());
    }

    public enum OAuthProvider {
        GOOGLE, MICROSOFT, LINKEDIN
    }
}