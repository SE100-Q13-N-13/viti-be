package com.example.viti_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWarrantyTicketRequest {

    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    private UUID customerId; // Nullable for guest

    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String customerName; // Required if customerId is null

    @Pattern(regexp = "^[0-9]{10,20}$", message = "Invalid phone number")
    private String customerPhone;

    @NotBlank(message = "Problem description is required")
    @Size(max = 1000, message = "Problem description must not exceed 1000 characters")
    private String problemDescription;

    private String accessories; // Phụ kiện đi kèm

    private UUID technicianId; // Optional, có thể assign sau

    private LocalDateTime expectedReturnDate;

    @Valid
    private List<ServiceItemRequest> services; // Dịch vụ ban đầu

    private String notes;
}