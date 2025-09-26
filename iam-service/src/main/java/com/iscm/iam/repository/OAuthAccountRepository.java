package com.iscm.iam.repository;

import com.iscm.iam.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, String provider);

    List<OAuthAccount> findByUserId(UUID userId);

    Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId);

    @Query("SELECT oa FROM OAuthAccount oa WHERE oa.user.email = :email")
    List<OAuthAccount> findByUserEmail(@Param("email") String email);

    @Query("SELECT oa FROM OAuthAccount oa WHERE oa.isActive = true AND oa.tokenExpiry < :now")
    List<OAuthAccount> findExpiredTokens(@Param("now") java.time.LocalDateTime now);

    void deleteAllByTokenExpiryBefore(java.time.LocalDateTime dateTime);
}