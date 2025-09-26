package com.iscm.iam.service;

import com.iscm.iam.dto.AuthRequest;
import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.RegisterRequest;
import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import com.iscm.iam.model.UserRole;
import com.iscm.iam.model.UserSession;
import com.iscm.iam.repository.OrganizationRepository;
import com.iscm.iam.repository.RoleRepository;
import com.iscm.iam.repository.UserRepository;
import com.iscm.iam.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @Transactional
    public AuthResponse login(AuthRequest request) {
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
        user.setTenantId(organization != null ? organization.getId() : null);

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
    public void logout(String refreshToken) {
        sessionService.revokeSession(refreshToken);
    }

    @Transactional
    public void logoutAllSessions(UUID userId) {
        sessionService.revokeAllUserSessions(userId);
    }

    private void handleFailedLogin(User user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        // Lock account after 5 failed attempts for 30 minutes
        if (newAttempts >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Account locked for user: {} due to {} failed attempts", 
                    user.getEmail(), newAttempts);
        }

        userRepository.save(user);
    }

    private Role getDefaultRoleForRegistration() {
        // If this is the first user, assign SUPER_ADMIN role
        if (userRepository.count() == 0) {
            return roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found"));
        }
        
        // Otherwise assign USER role (you might want to create this role in initial data)
        return roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.findByName("SUPER_ADMIN")
                        .orElseThrow(() -> new IllegalStateException("No default role found")));
    }
}