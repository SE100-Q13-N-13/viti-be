package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.OrderItemResponse;
import com.example.viti_be.model.OrderItem;
import com.example.viti_be.model.ProductVariant;

public class OrderItemMapper {
    public static OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        ProductVariant variant = item.getProductVariant();

        return OrderItemResponse.builder()
                .id(item.getId())
                .productVariantId(variant.getId())
                .sku(variant.getSku())
                .productName(variant.getProduct() != null ? variant.getProduct().getName() : "Unknown Product")
                .variantName(variant.getVariantName())
                // .productImage(variant.getImageUrl())

                // Serial info
                .serialNumber(item.getProductSerial() != null ? item.getProductSerial().getSerialNumber() : null)

                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .subtotal(item.getSubtotal())
                .warrantyExpireDate(item.getWarrantyPeriodSnapshot())
                .build();
    }
}
