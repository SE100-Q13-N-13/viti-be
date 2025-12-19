package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PurchaseOrderResponse {
    private UUID id;
    private String poNumber;
    private SupplierInfo supplier;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private List<PurchaseOrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    @Data
    @Builder
    public static class SupplierInfo {
        private UUID id;
        private String name;
        private String contactName;
        private String phone;
        private String email;
    }
}
