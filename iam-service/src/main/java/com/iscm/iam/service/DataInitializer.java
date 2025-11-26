package com.iscm.iam.service;

import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Permission;
import com.iscm.iam.model.Role;
import com.iscm.iam.repository.OrganizationRepository;
import com.iscm.iam.repository.PermissionRepository;
import com.iscm.iam.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Initializes basic data for the IAM service.
 * This ensures that essential roles and permissions exist when the application starts.
 */
@Slf4j
@Component
@Order(1) // Run this early in the startup process
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        try {
            // Initialize default organization
            Organization defaultOrg = initializeDefaultOrganization();

            // Initialize permissions
            initializePermissions();

            // Initialize roles
            initializeRoles(defaultOrg);

            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during data initialization", e);
            throw e;
        }
    }

    private Organization initializeDefaultOrganization() {
        return organizationRepository.findByName("ISCM Platform")
                .orElseGet(() -> {
                    Organization org = Organization.builder()
                            .name("ISCM Platform")
                            .domain("iscm-platform.com")
                            .status("ACTIVE")
                            .build();

                    Organization saved = organizationRepository.save(org);
                    log.info("Created default organization: {} with ID: {}", saved.getName(), saved.getId());
                    return saved;
                });
    }

    private void initializePermissions() {
        String[] permissionData = {
                "USER_READ:Read user information",
                "USER_WRITE:Create/update users",
                "USER_DELETE:Delete users",
                "ROLE_MANAGEMENT:Manage roles and permissions"
        };

        for (String permData : permissionData) {
            String[] parts = permData.split(":", 2);
            String code = parts[0];
            String description = parts[1];

            permissionRepository.findByCode(code)
                    .orElseGet(() -> {
                        Permission permission = Permission.builder()
                                .code(code)
                                .description(description)
                                .build();

                        Permission saved = permissionRepository.save(permission);
                        log.info("Created permission: {} with ID: {}", saved.getCode(), saved.getId());
                        return saved;
                    });
        }
    }

    private void initializeRoles(Organization defaultOrg) {
        // Initialize SUPER_ADMIN role
        roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> {
                    Role superAdminRole = Role.builder()
                            .name("SUPER_ADMIN")
                            .description("Super Administrator with full access")
                            .scope("PLATFORM")
                            .build();

                    Role saved = roleRepository.save(superAdminRole);
                    log.info("Created SUPER_ADMIN role with ID: {}", saved.getId());
                    return saved;
                });

        // Initialize ADMIN role
        roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role adminRole = Role.builder()
                            .name("ADMIN")
                            .description("Administrator with organizational access")
                            .scope("ORGANIZATION")
                            .build();

                    Role saved = roleRepository.save(adminRole);
                    log.info("Created ADMIN role with ID: {}", saved.getId());
                    return saved;
                });

        // Initialize USER role
        roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role userRole = Role.builder()
                            .name("USER")
                            .description("Regular user with basic access")
                            .scope("ORGANIZATION")
                            .build();

                    Role saved = roleRepository.save(userRole);
                    log.info("Created USER role with ID: {}", saved.getId());
                    return saved;
                });
    }

    private void assignPermissionsToRole(Role role, List<String> permissionCodes) {
        // For now, just log that permissions would be assigned
        // Role-permission assignments can be handled by a separate admin interface or Liquibase changesets
        log.info("Permissions {} would be assigned to role {}", permissionCodes, role.getName());
    }
}