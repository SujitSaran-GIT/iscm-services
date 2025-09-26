package com.iscm.iam.util;

import com.iscm.iam.dto.RegisterRequest;
import com.iscm.iam.model.Organization;
import com.iscm.iam.model.Role;
import com.iscm.iam.model.User;

import java.util.UUID;

public class TestDataFactory {

    public static User createTestUser() {
        User user = new User();
        user.setEmail("test-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("hashedpassword");
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    public static RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test-" + UUID.randomUUID() + "@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("Test");
        request.setLastName("User");
        return request;
    }

    public static Organization createTestOrganization() {
        Organization org = new Organization();
        org.setName("Test Org " + UUID.randomUUID());
        org.setDomain("test" + UUID.randomUUID() + ".com");
        return org;
    }

    public static Role createTestRole() {
        Role role = new Role();
        role.setName("TEST_ROLE_" + UUID.randomUUID());
        role.setDescription("Test Role");
        role.setScope("PLATFORM");
        return role;
    }
}