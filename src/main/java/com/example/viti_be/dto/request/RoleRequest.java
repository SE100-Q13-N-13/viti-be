package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "Role name must start with 'ROLE_' and contain only uppercase letters and underscores")
    @Size(min = 6, max = 50, message = "Role name must be between 6 and 50 characters")
    private String name; // ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CUSTOMER

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}