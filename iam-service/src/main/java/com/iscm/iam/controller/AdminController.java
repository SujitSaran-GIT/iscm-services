package com.iscm.iam.controller;

import com.iscm.iam.dto.AdminUserCreateRequest;
import com.iscm.iam.dto.AdminUserUpdateRequest;
import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.UserListResponse;
import com.iscm.iam.dto.UserStatisticsResponse;
import com.iscm.iam.model.User;
import com.iscm.iam.service.AdminService;
import com.iscm.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin user management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<UserListResponse> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,

            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
            Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<User> usersPage = adminService.getAllUsers(pageable);

        UserListResponse response = UserListResponse.builder()
                .users(usersPage.getContent().stream()
                        .map(AuthResponse.UserDto::fromEntity)
                        .toList())
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .total(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Get user by ID (Admin only)")
    public ResponseEntity<AuthResponse.UserDto> getUserById(@PathVariable UUID userId) {
        User user = userService.findById(userId);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(user);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create new user (Admin only)")
    public ResponseEntity<AuthResponse.UserDto> createUser(@RequestBody AdminUserCreateRequest request) {
        User user = adminService.createUser(request);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(user);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update user (Admin only)")
    public ResponseEntity<AuthResponse.UserDto> updateUser(
            @PathVariable UUID userId,
            @RequestBody AdminUserUpdateRequest request) {

        User user = adminService.updateUser(userId, request);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(user);
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/lock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lock user account (Admin only)")
    public ResponseEntity<Void> lockUser(
            @PathVariable UUID userId,
            @Parameter(description = "Lock duration in minutes", example = "30")
            @RequestParam(defaultValue = "30") long durationMinutes) {

        adminService.lockUser(userId, durationMinutes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Unlock user account (Admin only)")
    public ResponseEntity<Void> unlockUser(@PathVariable UUID userId) {
        adminService.unlockUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset user password (Admin only)")
    public ResponseEntity<Void> resetUserPassword(
            @PathVariable UUID userId,
            @Parameter(description = "New password", example = "NewSecurePass123!")
            @RequestParam String newPassword) {

        adminService.resetUserPassword(userId, newPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Search users by email or name (Admin only)")
    public ResponseEntity<UserListResponse> searchUsers(
            @Parameter(description = "Search term", example = "john")
            @RequestParam String query,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = adminService.searchUsers(query, pageable);

        UserListResponse response = UserListResponse.builder()
                .users(usersPage.getContent().stream()
                        .map(AuthResponse.UserDto::fromEntity)
                        .toList())
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .total(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get user statistics (Admin only)")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        UserStatisticsResponse statistics = adminService.getUserStatistics();
        return ResponseEntity.ok(statistics);
    }
}