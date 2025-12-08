package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierRequest {
    @NotBlank(message = "Supplier name is required")
    private String name;
    private String contact_name;
    private String phone;
    private String address;
    private String email;
}
