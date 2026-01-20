package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPromotionCodeRequest {

    // ===== CART INFORMATION =====
    private UUID customerId; // Nullable for guest

    @NotNull(message = "Items cannot be null")
    @NotEmpty(message = "Items cannot be empty")
    private List<CartItemRequest> items;

    /**
     * Code mới muốn apply (dùng cho endpoint /apply-code)
     * Nullable - chỉ có giá trị khi user muốn thêm code mới
     */
    private String code;

    /**
     * Danh sách codes đã được apply vào cart
     * Dùng để tính discount tổng hợp
     */
    @Builder.Default
    private List<String> appliedCodes = new ArrayList<>();
}
