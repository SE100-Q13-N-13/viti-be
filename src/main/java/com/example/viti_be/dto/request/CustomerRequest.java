package com.example.viti_be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomerRequest {

    private UUID tierId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\d{10,11}$", message = "Phone must be 10-11 digits")
    private String phone;

    @Email(message = "Email must be valid")
    private String email;
}
