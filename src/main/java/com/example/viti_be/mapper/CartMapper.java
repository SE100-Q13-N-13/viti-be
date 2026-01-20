package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.CartItemResponse;
import com.example.viti_be.dto.response.CartResponse;
import com.example.viti_be.model.Cart;
import com.example.viti_be.model.CartItem;
import com.example.viti_be.model.Inventory;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper để convert Cart entities sang DTOs
 */
public class CartMapper {

    /**
     * Map Cart entity sang CartResponse
     * @param cart Cart entity
     * @param inventoryMap Map<VariantId, Inventory> để lấy thông tin tồn kho
     * @return CartResponse
     */
    public static CartResponse mapToCartResponse(Cart cart, Map<UUID, Inventory> inventoryMap) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .filter(item -> !item.getIsDeleted())
                .map(item -> mapToCartItemResponse(item, inventoryMap.get(item.getProductVariant().getId())))
                .collect(Collectors.toList());

        // Tính tổng số lượng và tổng tiền
        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomer().getId())
                .customerName(cart.getCustomer().getFullName())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    /**
     * Map CartItem entity sang CartItemResponse
     * @param cartItem CartItem entity
     * @param inventory Inventory của variant (có thể null)
     * @return CartItemResponse
     */
    public static CartItemResponse mapToCartItemResponse(CartItem cartItem, Inventory inventory) {
        if (cartItem == null) {
            return null;
        }

        ProductVariant variant = cartItem.getProductVariant();
        Product product = variant.getProduct();

        // Kiểm tra giá có thay đổi không
        BigDecimal currentPrice = variant.getSellingPrice();
        Boolean priceChanged = cartItem.getUnitPrice() != null && currentPrice != null 
                && cartItem.getUnitPrice().compareTo(currentPrice) != 0;

        // Lấy số lượng tồn kho available
        Integer availableStock = inventory != null ? inventory.getQuantityAvailable() : 0;

        // Kiểm tra sản phẩm còn hoạt động không
        Boolean isAvailable = "ACTIVE".equalsIgnoreCase(product.getStatus()) && !product.getIsDeleted();

        return CartItemResponse.builder()
                .id(cartItem.getId())
                // Product info
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .productStatus(product.getStatus())
                .warrantyPeriod(product.getWarrantyPeriod())
                // Variant info
                .productVariantId(variant.getId())
                .variantName(variant.getVariantName())
                .variantSpecs(variant.getVariantSpecs())
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                // Price & quantity
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .currentPrice(currentPrice)
                .priceChanged(priceChanged)
                .subtotal(cartItem.getSubtotal())
                // Stock info
                .availableStock(availableStock)
                .isAvailable(isAvailable)
                .addedAt(cartItem.getCreatedAt())
                .build();
    }
}
