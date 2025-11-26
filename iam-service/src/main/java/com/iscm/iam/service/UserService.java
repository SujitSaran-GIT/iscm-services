package com.iscm.iam.service;

import com.iscm.iam.dto.UserUpdateRequest;
import com.iscm.iam.exception.UserDeletionException;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import com.iscm.iam.model.UserRole;
import com.iscm.iam.repository.DeviceRepository;
import com.iscm.iam.repository.OAuthAccountRepository;
import com.iscm.iam.repository.PasswordResetTokenRepository;
import com.iscm.iam.repository.RoleRepository;
import com.iscm.iam.repository.UserRepository;
import com.iscm.iam.repository.UserRoleRepository;
import com.iscm.iam.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DeviceRepository deviceRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithAllDetails(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildUserDetailsOptimized(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUserId(String userId) {
        User user = userRepository.findByIdWithAllDetails(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return buildUserDetailsOptimized(user);
    }

    private UserDetails buildUserDetailsOptimized(User user) {
        Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();

        // Extract role IDs to fetch permissions separately (avoid MultipleBagFetchException)
        List<UUID> roleIds = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getId())
                .collect(Collectors.toList());

        // Fetch roles with permissions in a separate query
        if (!roleIds.isEmpty()) {
            List<Role> rolesWithPermissions = roleRepository.findRolesWithPermissions(roleIds);

            // Create a map of role ID to Role with permissions
            Map<UUID, Role> roleMap = rolesWithPermissions.stream()
                    .collect(Collectors.toMap(Role::getId, role -> role));

            // Build authorities
            user.getUserRoles().forEach(userRole -> {
                Role role = roleMap.get(userRole.getRole().getId());
                if (role != null) {
                    // Add role authority
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                    // Add permission authorities
                    role.getPermissions().forEach(permission ->
                        authorities.add(new SimpleGrantedAuthority(permission.getCode()))
                    );
                }
            });
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.getIsActive(),
                true, // account non-expired
                true, // credentials non-expired
                user.isAccountNonLocked(),
                authorities
        );
    }

    @Transactional
    public void assignRoleToUser(User user, Role role, UUID assignedBy) {
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(LocalDateTime.now());
        userRole.setAssignedBy(assignedBy);
        // Set tenantId from user to ensure it's not null
        userRole.setTenantId(user.getTenantId());
        user.getUserRoles().add(userRole);
        userRepository.save(user);
    }

    @Transactional
    public void assignDefaultRoleToUser(User user) {
        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default USER role not found"));
        assignRoleToUser(user, defaultRole, null);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User updateUser(UUID userId, UserUpdateRequest updateRequest) {
        User user = findById(userId);

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (existsByEmail(updateRequest.getEmail())) {
                throw new RuntimeException("Email already exists: " + updateRequest.getEmail());
            }
            user.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getPhoneNumber() != null) {
            user.setPhone(updateRequest.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findById(userId);
        userRepository.delete(user);
    }

    @Transactional
    public User updateCurrentUser(String email, UserUpdateRequest updateRequest) {
        User user = findByEmail(email);

        // Update email if provided and different
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (existsByEmail(updateRequest.getEmail())) {
                throw new RuntimeException("Email already exists: " + updateRequest.getEmail());
            }
            user.setEmail(updateRequest.getEmail());
        }

        // Update first name if provided
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }

        // Update last name if provided
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }

        // Update phone number if provided
        if (updateRequest.getPhoneNumber() != null) {
            user.setPhone(updateRequest.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteCurrentUser(String email) {
        log.info("Starting deletion process for user: {}", email);

        try {
            // Find the user
            User user = findByEmail(email);
            UUID userId = user.getId();

            // Delete user roles
            log.debug("Deleting user roles for user: {}", userId);
            userRoleRepository.deleteByUserId(userId);

            // Delete user devices
            log.debug("Deleting user devices for user: {}", userId);
            deviceRepository.deleteByUserId(userId);

            // Delete OAuth accounts
            log.debug("Deleting OAuth accounts for user: {}", userId);
            oauthAccountRepository.deleteByUserId(userId);

            // Delete password reset tokens
            log.debug("Deleting password reset tokens for user: {}", userId);
            passwordResetTokenRepository.deleteByUserId(userId);

            // Delete user sessions
            log.debug("Deleting user sessions for user: {}", userId);
            userSessionRepository.deleteByUserId(userId);

            // Finally delete the user
            log.debug("Deleting user: {}", userId);
            userRepository.delete(user);

            log.info("Successfully deleted user and all related data for: {}", email);

        } catch (UsernameNotFoundException e) {
            log.error("User not found for deletion: {}", email);
            throw new UserDeletionException("User not found: " + email, e);
        } catch (Exception e) {
            log.error("Error deleting user {}: {}", email, e.getMessage(), e);
            throw new UserDeletionException("Failed to delete user profile: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the authenticated user is the same as the user with the given userId
     */
    @Transactional(readOnly = true)
    public boolean isCurrentUser(org.springframework.security.core.Authentication authentication, UUID userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String currentUserEmail = authentication.getName();
        try {
            User currentUser = findByEmail(currentUserEmail);
            return currentUser.getId().equals(userId);
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }
}