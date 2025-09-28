package com.iscm.iam.controller;

import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.UserUpdateRequest;
import com.iscm.iam.exception.UserDeletionException;
import com.iscm.iam.model.User;
import com.iscm.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

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
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(user);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<AuthResponse.UserDto> getUserById(@PathVariable UUID userId) {
        User user = userService.findById(userId);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(user);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<AuthResponse.UserDto> updateCurrentUser(@Valid @RequestBody UserUpdateRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User updatedUser = userService.updateCurrentUser(email, updateRequest);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Update user")
    public ResponseEntity<AuthResponse.UserDto> updateUser(@PathVariable UUID userId, @Valid @RequestBody UserUpdateRequest updateRequest) {
        User updatedUser = userService.updateUser(userId, updateRequest);
        AuthResponse.UserDto userDto = AuthResponse.UserDto.fromEntity(updatedUser);
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user profile")
    public ResponseEntity<Void> deleteCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        try {
            userService.deleteCurrentUser(email);
            return ResponseEntity.noContent().build();
        } catch (UserDeletionException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete user profile: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}