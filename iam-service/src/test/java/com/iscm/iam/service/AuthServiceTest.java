package com.iscm.iam.service;

import com.iscm.iam.BaseIntegrationTest;
import com.iscm.iam.dto.AuthRequest;
import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.RegisterRequest;
import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import com.iscm.iam.repository.OrganizationRepository;
import com.iscm.iam.repository.RoleRepository;
import com.iscm.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        // Create test role
        userRole = new Role();
        userRole.setName("TEST_USER");
        userRole.setDescription("Test User Role");
        userRole.setScope("PLATFORM");
        userRole = roleRepository.save(userRole);

        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setDomain("test.org");
        testOrganization = organizationRepository.save(testOrganization);
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setOrganizationId(testOrganization.getId());

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("John", response.getUser().getFirstName());
        assertEquals("Doe", response.getUser().getLastName());

        // Verify user is saved in database
        Optional<User> savedUser = userRepository.findByEmail("test@example.com");
        assertTrue(savedUser.isPresent());
        assertTrue(passwordEncoder.matches("SecurePass123!", savedUser.get().getPasswordHash()));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        // Create user first
        authService.register(request);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });
    }

    @Test
    void testLogin_Success() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("Login");
        registerRequest.setLastName("User");
        authService.register(registerRequest);

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("SecurePass123!");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("login@example.com", response.getUser().getEmail());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("wrongpassword");

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void testLogin_AccountLockedAfterMultipleFailures() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("lock@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("Lock");
        registerRequest.setLastName("User");
        authService.register(registerRequest);

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("lock@example.com");
        loginRequest.setPassword("wrongpassword");

        // When - Attempt login multiple times
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(loginRequest);
            } catch (BadCredentialsException ignored) {
                // Expected
            }
        }

        // Then - Account should be locked
        assertThrows(LockedException.class, () -> {
            authService.login(loginRequest);
        });

        // Verify user is locked in database
        User lockedUser = userRepository.findByEmail("lock@example.com").orElseThrow();
        assertNotNull(lockedUser.getAccountLockedUntil());
        assertTrue(lockedUser.getFailedLoginAttempts() >= 5);
    }

    @Test
    void testRefreshToken_Success() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("refresh@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("Refresh");
        registerRequest.setLastName("User");
        AuthResponse registerResponse = authService.register(registerRequest);

        // When
        AuthResponse refreshResponse = authService.refreshToken(registerResponse.getRefreshToken());

        // Then
        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getAccessToken());
        assertNotNull(refreshResponse.getRefreshToken());
        assertNotEquals(registerResponse.getAccessToken(), refreshResponse.getAccessToken());
        assertNotEquals(registerResponse.getRefreshToken(), refreshResponse.getRefreshToken());
    }

    @Test
    void testLogout_Success() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("logout@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("Logout");
        registerRequest.setLastName("User");
        AuthResponse registerResponse = authService.register(registerRequest);

        // When - No exception should be thrown
        assertDoesNotThrow(() -> {
            authService.logout(registerResponse.getRefreshToken());
        });

        // Then - Attempting to refresh with revoked token should fail
        assertThrows(SecurityException.class, () -> {
            authService.refreshToken(registerResponse.getRefreshToken());
        });
    }
}