package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.WarrantyTicketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyTicketResponse {

    private UUID id;
    private String ticketNumber;

    // Product info
    private ProductInfo product;
    private String serialNumber;

    // Customer info
    private CustomerInfo customer;

    // Technician info
    private TechnicianInfo technician;

    // Problem
    private String problemDescription;
    private String accessories;

    // Warranty status
    private Boolean isUnderWarranty;
    private LocalDateTime warrantyExpireDate;

    // Status
    private WarrantyTicketStatus status;

    // Dates
    private LocalDateTime receivedDate;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime actualReturnDate;

    // Costs
    private BigDecimal totalServiceCost;
    private BigDecimal totalPartCost;
    private BigDecimal totalCost;

    // Details
    private List<ServiceItemResponse> services;
    private List<PartItemResponse> parts;

    // Notes
    private String notes;
    private String cancellationReason;

    // Meta
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested DTOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductInfo {
        private UUID variantId;
        private String sku;
        private String productName;
        private String variantName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerInfo {
        private UUID customerId;
        private String name;
        private String phone;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicianInfo {
        private UUID userId;
        private String fullName;
        private String email;
    }
}
