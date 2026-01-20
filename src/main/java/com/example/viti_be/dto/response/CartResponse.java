package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response trả về thông tin giỏ hàng đầy đủ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    
    private UUID id;
    
    private UUID customerId;
    
    private String customerName;
    
    private List<CartItemResponse> items;
    
    /**
     * Tổng số lượng sản phẩm trong giỏ
     */
    private Integer totalItems;
    
    /**
     * Tổng tiền giỏ hàng
     */
    private BigDecimal totalAmount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
