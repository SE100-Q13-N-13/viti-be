package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request để thêm sản phẩm vào giỏ hàng
 * Mỗi lần thêm sẽ cộng 1 quantity vào giỏ hàng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    
    @NotNull(message = "Product variant ID không được để trống")
    private UUID productVariantId;
}
