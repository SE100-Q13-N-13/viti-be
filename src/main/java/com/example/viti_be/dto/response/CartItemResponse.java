package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response trả về thông tin chi tiết của một item trong giỏ hàng
 * Bao gồm đầy đủ thông tin sản phẩm và variant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    
    private UUID id;
    
    /**
     * Thông tin Product
     */
    private UUID productId;
    private String productName;
    private String productImageUrl;
    private String productStatus;
    private Integer warrantyPeriod;
    
    /**
     * Thông tin Product Variant
     */
    private UUID productVariantId;
    private String variantName;
    private String variantSpecs;
    private String sku;
    private String barcode;
    
    /**
     * Thông tin giá và số lượng
     */
    private Integer quantity;
    private BigDecimal unitPrice;
    
    /**
     * Giá hiện tại của variant (để so sánh nếu giá đã thay đổi)
     */
    private BigDecimal currentPrice;
    
    /**
     * Giá đã thay đổi so với lúc thêm vào giỏ
     */
    private Boolean priceChanged;
    
    /**
     * Tổng tiền của item (quantity * unitPrice)
     */
    private BigDecimal subtotal;
    
    /**
     * Tình trạng tồn kho (để kiểm tra có đủ hàng không)
     */
    private Integer availableStock;
    
    /**
     * Sản phẩm còn hoạt động không (ACTIVE/HIDDEN)
     */
    private Boolean isAvailable;
    
    private LocalDateTime addedAt;
}
