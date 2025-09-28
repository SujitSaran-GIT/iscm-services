package com.iscm.iam.service;

import com.iscm.iam.dto.AdminUserCreateRequest;
import com.iscm.iam.dto.AdminUserUpdateRequest;
import com.iscm.iam.dto.UserStatisticsResponse;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import com.iscm.iam.model.UserRole;
import com.iscm.iam.repository.RoleRepository;
import com.iscm.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordService passwordService;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                query, query, query, pageable);
    }

    @Transactional
    public User createUser(AdminUserCreateRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(request.getActive());
        user.setMfaEnabled(request.getMfaEnabled());
        user.setAuthProvider("LOCAL");
        user.setTenantId(request.getTenantId());

        // Save user
        user = userRepository.save(user);

        // Assign roles if provided
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            assignRolesToUser(user, request.getRoles());
        } else {
            // Assign default USER role
            userService.assignDefaultRoleToUser(user);
        }

        log.info("Admin created new user: {}", user.getEmail());
        return user;
    }

    @Transactional
    public User updateUser(UUID userId, AdminUserUpdateRequest request) {
        User user = userService.findById(userId);

        // Update user fields if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if new email is already taken
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("User with email " + request.getEmail() + " already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getActive() != null) {
            user.setIsActive(request.getActive());
        }

        if (request.getMfaEnabled() != null) {
            user.setMfaEnabled(request.getMfaEnabled());
        }

        if (request.getTenantId() != null) {
            user.setTenantId(request.getTenantId());
        }

        // Update roles if provided
        if (request.getRoles() != null) {
            // Clear existing roles
            user.getUserRoles().clear();
            // Assign new roles
            assignRolesToUser(user, request.getRoles());
        }

        user = userRepository.save(user);
        log.info("Admin updated user: {}", user.getEmail());
        return user;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userService.findById(userId);

        // Prevent deletion of the last super admin
        if (isLastSuperAdmin(user)) {
            throw new RuntimeException("Cannot delete the last super admin user");
        }

        userRepository.delete(user);
        log.info("Admin deleted user: {}", user.getEmail());
    }

    @Transactional
    public void lockUser(UUID userId, long durationMinutes) {
        User user = userService.findById(userId);

        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        user.setAccountLockedUntil(lockUntil);

        userRepository.save(user);
        log.info("Admin locked user {} until {}", user.getEmail(), lockUntil);
    }

    @Transactional
    public void unlockUser(UUID userId) {
        User user = userService.findById(userId);
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);

        userRepository.save(user);
        log.info("Admin unlocked user: {}", user.getEmail());
    }

    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        User user = userService.findById(userId);

        // Validate new password
        if (!passwordService.isValidPassword(newPassword)) {
            throw new RuntimeException("New password does not meet security requirements");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Admin reset password for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime weekStart = now.minusWeeks(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime monthStart = now.minusMonths(1).truncatedTo(ChronoUnit.DAYS);

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long inactiveUsers = totalUsers - activeUsers;
        long lockedUsers = userRepository.countByAccountLockedUntilAfter(LocalDateTime.now());
        long mfaEnabledUsers = userRepository.countByMfaEnabled(true);

        long registeredToday = userRepository.countByCreatedAtAfter(todayStart);
        long registeredThisWeek = userRepository.countByCreatedAtAfter(weekStart);
        long registeredThisMonth = userRepository.countByCreatedAtAfter(monthStart);

        // Count users by role
        long superAdminCount = userRepository.countByRoleName("SUPER_ADMIN");
        long adminCount = userRepository.countByRoleName("ADMIN");
        long regularUserCount = userRepository.countByRoleName("USER");

        return UserStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .lockedUsers(lockedUsers)
                .mfaEnabledUsers(mfaEnabledUsers)
                .registeredToday(registeredToday)
                .registeredThisWeek(registeredThisWeek)
                .registeredThisMonth(registeredThisMonth)
                .superAdminCount(superAdminCount)
                .adminCount(adminCount)
                .regularUserCount(regularUserCount)
                .build();
    }

    private void assignRolesToUser(User user, List<String> roleNames) {
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role " + roleName + " not found"));

            // Check if user already has this role
            boolean alreadyHasRole = user.getUserRoles().stream()
                    .anyMatch(userRole -> userRole.getRole().getName().equals(roleName));

            if (!alreadyHasRole) {
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                userRole.setAssignedAt(LocalDateTime.now());
                userRole.setAssignedBy(null); // System-assigned
                user.getUserRoles().add(userRole);
            }
        }
    }

    private boolean isLastSuperAdmin(User user) {
        boolean isSuperAdmin = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getName().equals("SUPER_ADMIN"));

        if (!isSuperAdmin) {
            return false;
        }

        long superAdminCount = userRepository.countByRoleName("SUPER_ADMIN");
        return superAdminCount <= 1;
    }
}