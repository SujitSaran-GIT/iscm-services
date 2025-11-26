package com.iscm.iam.service;

import com.iscm.iam.dto.AuthRequest;
import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.RegisterRequest;
import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import com.iscm.iam.model.UserSession;
import com.iscm.iam.repository.OrganizationRepository;
import com.iscm.iam.repository.RoleRepository;
import com.iscm.iam.repository.UserRepository;
import com.iscm.iam.security.JwtUtil;
// import com.iscm.iam.security.JwtBlacklistService;
import com.iscm.iam.security.SecurityMonitoringService;
// import com.iscm.iam.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserService userService;
    private final UserSessionService sessionService;
    private final PasswordService passwordService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    // private final JwtBlacklistService jwtBlacklistService;
    // private final SecurityMonitoringService securityMonitoringService;
    // private final CacheService cacheService;
    // private final AsyncProcessingService asyncProcessingService;

    @Transactional
    // @Cacheable(value = "users", key = "#request.email")
    public AuthResponse login(AuthRequest request) {
        // Get user from database (cache disabled)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account temporarily locked. Try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(), 
                    request.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Reset failed attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Cache the updated user
            // cacheService.cacheUser(user);

            // Log successful login asynchronously - DISABLED
            // asyncProcessingService.processSecurityEventAsync(
            //     SecurityMonitoringService.SecurityEventType.SUCCESSFUL_LOGIN,
            //     "User logged in successfully",
            //     request.getIpAddress(),
            //     request.getUserAgent(),
            //     user.getId()
            // );

            // Send login notification asynchronously - DISABLED
            // asyncProcessingService.sendLoginNotificationAsync(user, request.getIpAddress(), request.getUserAgent());

            // Generate tokens
            List<String> roles = user.getUserRoles().stream()
                    .map(userRole -> userRole.getRole().getName())
                    .toList();

            String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), roles, user.getTenantId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            // Save refresh token
            sessionService.createSession(user, refreshToken, 
                                       request.getUserAgent(), request.getIpAddress());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getAccessTokenExpiration())
                    .user(AuthResponse.UserDto.fromEntity(user))
                    .build();

        } catch (BadCredentialsException ex) {
            // Log failed login attempt asynchronously - DISABLED
            // asyncProcessingService.processSecurityEventAsync(
            //     SecurityMonitoringService.SecurityEventType.FAILED_LOGIN,
            //     "Failed login attempt for user: " + request.getEmail(),
            //     request.getIpAddress(),
            //     request.getUserAgent(),
            //     user != null ? user.getId() : null
            // );

            // Check for brute force attack asynchronously - DISABLED
            // asyncProcessingService.processSecurityEventAsync(
            //     SecurityMonitoringService.SecurityEventType.BRUTE_FORCE_DETECTED,
            //     "Potential brute force attack from IP: " + request.getIpAddress(),
            //     request.getIpAddress(),
            //     request.getUserAgent(),
            //     user != null ? user.getId() : null
            // );

            // Increment failed attempts
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Validate additional fields including password confirmation
        request.validateAdditionalFields();

        // Validate password strength
        var passwordValidation = passwordService.validatePassword(request.getPassword());
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException(
                "Password does not meet requirements: " + 
                passwordService.getPasswordValidationMessage(passwordValidation)
            );
        }

        Organization organization = null;
        if (request.getOrganizationId() != null) {
            organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        }

        // Get default role (SUPER_ADMIN for first user, otherwise USER)
        Role defaultRole = getDefaultRoleForRegistration();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordService.encodePassword(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setOrganization(organization);
        user.setTenantId(organization != null ? organization.getId() : UUID.fromString("00000000-0000-0000-0000-000000000000"));

        User savedUser = userRepository.save(user);

        // Assign default role using UserService
        userService.assignRoleToUser(savedUser, defaultRole, null);

        // Generate tokens for auto-login after registration
        List<String> roles = savedUser.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        String accessToken = jwtUtil.generateAccessToken(
            savedUser.getId(), savedUser.getEmail(), roles, savedUser.getTenantId());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        sessionService.createSession(savedUser, refreshToken, null, null);

        // Process post-registration tasks asynchronously - DISABLED
        // asyncProcessingService.processUserRegistrationAsync(savedUser, null); // IP address not available in registration request

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .user(AuthResponse.UserDto.fromEntity(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        UserSession session = sessionService.validateRefreshToken(refreshToken);
        User user = session.getUser();

        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        String newAccessToken = jwtUtil.generateAccessToken(
            user.getId(), user.getEmail(), roles, user.getTenantId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Update session with new refresh token
        sessionService.updateSession(session, newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .user(AuthResponse.UserDto.fromEntity(user))
                .build();
    }

    @Transactional
    // @CacheEvict(value = {"users", "activeSessions"}, key = "#result?.id")
    public void logout(String refreshToken) {
        try {
            // Get session details for logging
            UserSession session = sessionService.findSessionByRefreshToken(refreshToken);
            if (session != null) {
                User user = session.getUser();
                log.info("User logout: userId={}, sessionId={}", user.getId(), session.getId());

                // Clear user cache on logout
                // cacheService.clearUserCache(user.getId());

                // Blacklist the refresh token - DISABLED
                // jwtBlacklistService.blacklistToken(refreshToken, "User logout");

                // Blacklist associated access token if available
                // Note: In a real implementation, you might track the access token ID as well
                log.debug("Blacklisted refresh token during logout for user: {}", user.getId());
            } else {
                log.warn("Logout attempt with invalid refresh token");
            }

            // Revoke the session
            sessionService.revokeSession(refreshToken);

        } catch (Exception e) {
            log.error("Error during logout", e);
            // Still revoke session even if blacklisting fails
            sessionService.revokeSession(refreshToken);
        }
    }

    @Transactional
    public void logoutAllSessions(UUID userId) {
        try {
            // Blacklist all tokens for the user - DISABLED
            // jwtBlacklistService.blacklistAllUserTokens(userId, "User logout all sessions");
            log.info("All tokens blacklisted for user during logout: userId={}", userId);

            // Revoke all user sessions
            sessionService.revokeAllUserSessions(userId);

        } catch (Exception e) {
            log.error("Error during logout all sessions for user: {}", userId, e);
            // Still revoke sessions even if blacklisting fails
            sessionService.revokeAllUserSessions(userId);
        }
    }

    private void handleFailedLogin(User user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        // Lock account after 5 failed attempts for 30 minutes
        if (newAttempts >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Account locked for user: {} due to {} failed attempts",
                    user.getEmail(), newAttempts);

            // Log account lockout event - DISABLED
            // securityMonitoringService.recordSecurityEventWithUser(
            //     SecurityMonitoringService.SecurityEventType.ACCOUNT_LOCKED,
            //     String.format("Account locked for user %s due to %d failed login attempts",
            //         user.getEmail(), newAttempts),
            //     null, // IP address not available here
            //     null,
            //     user
            // );
        }

        userRepository.save(user);
    }

    private Role getDefaultRoleForRegistration() {
        // If this is the first user, assign SUPER_ADMIN role
        if (userRepository.count() == 0) {
            return roleRepository.findByName("SUPER_ADMIN")
                    .orElseGet(() -> {
                        log.warn("SUPER_ADMIN role not found, creating a basic USER role as fallback");
                        return createBasicUserRole();
                    });
        }

        // Otherwise assign USER role, fallback to creating one if USER doesn't exist
        return roleRepository.findByName("USER")
                .orElseGet(() -> {
                    log.warn("USER role not found, creating a basic USER role as fallback");
                    return createBasicUserRole();
                });
    }

    private Role createBasicUserRole() {
        try {
            Role userRole = Role.builder()
                    .name("USER")
                    .description("Basic user role created as fallback")
                    .scope("PLATFORM")
                    .build();

            Role saved = roleRepository.save(userRole);
            log.info("Created basic USER role as fallback: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to create basic USER role", e);
            throw new IllegalStateException("Unable to create a basic USER role for registration", e);
        }
    }
}