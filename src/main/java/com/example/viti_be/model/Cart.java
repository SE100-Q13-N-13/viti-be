package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /**
     * Tìm cart item theo product variant id
     */
    public CartItem findItemByVariantId(java.util.UUID variantId) {
        return items.stream()
                .filter(item -> item.getProductVariant().getId().equals(variantId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Thêm item vào giỏ hàng
     */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    /**
     * Xóa item khỏi giỏ hàng
     */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    /**
     * Xóa tất cả items trong giỏ hàng
     */
    public void clearItems() {
        items.forEach(item -> item.setCart(null));
        items.clear();
    }
}
