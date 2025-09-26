package com.iscm.iam.repository;

import com.iscm.iam.model.PasswordResetToken;
import com.iscm.iam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserAndIsUsedFalse(User user);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.isUsed = false")
    List<PasswordResetToken> findByUserIdAndIsUsedFalse(@Param("userId") UUID userId);

    void deleteAllByExpiresAtBefore(java.time.LocalDateTime dateTime);
}