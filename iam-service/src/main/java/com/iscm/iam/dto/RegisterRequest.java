package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "User registration request payload")
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 254, message = "Email must be less than 254 characters")
    @Schema(description = "User's email address", example = "new.user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character")
    @Schema(description = "User's password (min 8 characters with uppercase, lowercase, digit, and special character)",
            example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 128)
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Password confirmation must match the password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Size(max = 50, message = "Last name must be less than 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]*$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +1234567890)")
    @Schema(description = "User's phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "Organization ID for user association", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID organizationId;

    // Custom validation method for additional security checks
    public void validateAdditionalFields() {
        // Additional validation logic will be handled by SecurityValidator in service layer

        // Validate password confirmation
        if (password != null && confirmPassword != null && !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }
    }
}