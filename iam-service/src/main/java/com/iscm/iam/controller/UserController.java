package com.iscm.iam.controller;

import com.iscm.iam.model.User;
import com.iscm.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<User> getCurrentUser() {
        // Implementation would get user from security context
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<User> getUserById(@PathVariable UUID userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Update user")
    public ResponseEntity<User> updateUser(@PathVariable UUID userId, @RequestBody User userDetails) {
        // Implementation would update user
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        // Implementation would delete user
        return ResponseEntity.ok().build();
    }
}