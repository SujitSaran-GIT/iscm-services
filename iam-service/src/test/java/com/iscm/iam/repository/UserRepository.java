package com.iscm.iam.repository;

import com.iscm.iam.BaseIntegrationTest;
import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Organization testOrg;
    private Role testRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();

        testOrg = new Organization();
        testOrg.setName("Test Org");
        testOrg.setDomain("test.org");
        testOrg = organizationRepository.save(testOrg);

        testRole = new Role();
        testRole.setName("TEST_ROLE");
        testRole.setDescription("Test Role");
        testRole = roleRepository.save(testRole);
    }

    @Test
    void testSaveAndFindUser() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpassword");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setOrganization(testOrg);
        user.setRoles(List.of(testRole));

        // When
        User savedUser = userRepository.save(user);
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("test@example.com", foundUser.get().getEmail());
        assertEquals("John", foundUser.get().getFirstName());
        assertEquals(testOrg.getId(), foundUser.get().getOrganization().getId());
        assertFalse(foundUser.get().getRoles().isEmpty());
        assertEquals("TEST_ROLE", foundUser.get().getRoles().get(0).getName());
    }

    @Test
    void testFindByEmailWithRoles() {
        // Given
        User user = new User();
        user.setEmail("withroles@example.com");
        user.setPasswordHash("hashedpassword");
        user.setFirstName("With");
        user.setLastName("Roles");
        user.setRoles(List.of(testRole));
        userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findByEmailWithRoles("withroles@example.com");

        // Then
        assertTrue(foundUser.isPresent());
        assertFalse(foundUser.get().getRoles().isEmpty());
        assertEquals("TEST_ROLE", foundUser.get().getRoles().get(0).getName());
    }

    @Test
    void testExistsByEmail() {
        // Given
        User user = new User();
        user.setEmail("exists@example.com");
        user.setPasswordHash("hashedpassword");
        user.setFirstName("Exists");
        user.setLastName("Test");
        userRepository.save(user);

        // When & Then
        assertTrue(userRepository.existsByEmail("exists@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }
}