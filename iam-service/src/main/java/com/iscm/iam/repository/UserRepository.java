package com.iscm.iam.repository;

import com.iscm.iam.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);
    
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id != :id")
    Boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") UUID id);

    // Search methods
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email, String firstName, String lastName, Pageable pageable);

    // Count methods for statistics
    long countByIsActive(Boolean active);

    long countByAccountLockedUntilAfter(LocalDateTime dateTime);

    long countByMfaEnabled(Boolean mfaEnabled);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
}