package com.example.viti_be.repository;

import com.example.viti_be.model.Cart;
import com.example.viti_be.model.CartItem;
import com.example.viti_be.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    
    /**
     * Tìm cart item theo cart và product variant
     */
    Optional<CartItem> findByCartAndProductVariant(Cart cart, ProductVariant productVariant);
    
    /**
     * Tìm cart item theo cart ID và product variant ID
     */
    Optional<CartItem> findByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);
    
    /**
     * Tìm cart item theo cart ID và product variant ID, chưa bị xóa
     */
    Optional<CartItem> findByCartIdAndProductVariantIdAndIsDeletedFalse(UUID cartId, UUID productVariantId);
    
    /**
     * Lấy tất cả items của một giỏ hàng
     */
    List<CartItem> findByCartIdAndIsDeletedFalse(UUID cartId);
    
    /**
     * Đếm số items trong giỏ hàng
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.isDeleted = false")
    Integer countTotalItemsByCartId(@Param("cartId") UUID cartId);
    
    /**
     * Xóa tất cả items của một giỏ hàng
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") UUID cartId);
    
    /**
     * Xóa item theo cart ID và product variant ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productVariant.id = :variantId")
    void deleteByCartIdAndProductVariantId(@Param("cartId") UUID cartId, @Param("variantId") UUID variantId);
    
    /**
     * Kiểm tra product variant đã có trong giỏ hàng chưa
     */
    boolean existsByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);
}
