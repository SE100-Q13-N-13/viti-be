package com.example.viti_be.repository;

import com.example.viti_be.model.Cart;
import com.example.viti_be.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    
    /**
     * Tìm giỏ hàng theo customer
     */
    Optional<Cart> findByCustomer(Customer customer);
    
    /**
     * Tìm giỏ hàng theo customer ID
     */
    Optional<Cart> findByCustomerId(UUID customerId);
    
    /**
     * Tìm giỏ hàng theo customer ID và chưa bị xóa
     */
    Optional<Cart> findByCustomerIdAndIsDeletedFalse(UUID customerId);
    
    /**
     * Tìm giỏ hàng theo customer và chưa bị xóa, fetch eager các items
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.productVariant pv LEFT JOIN FETCH pv.product WHERE c.customer.id = :customerId AND c.isDeleted = false")
    Optional<Cart> findByCustomerIdWithItems(@Param("customerId") UUID customerId);
    
    /**
     * Kiểm tra customer đã có giỏ hàng chưa
     */
    boolean existsByCustomerIdAndIsDeletedFalse(UUID customerId);

    // ==================== CART TOKEN METHODS (for guest users) ====================

    /**
     * Tìm giỏ hàng theo cart token và chưa bị xóa
     */
    Optional<Cart> findByCartTokenAndIsDeletedFalse(String cartToken);

    /**
     * Tìm giỏ hàng theo cart token, fetch eager các items
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.productVariant pv LEFT JOIN FETCH pv.product WHERE c.cartToken = :cartToken AND c.isDeleted = false")
    Optional<Cart> findByCartTokenWithItems(@Param("cartToken") String cartToken);

    /**
     * Kiểm tra cart token đã tồn tại chưa
     */
    boolean existsByCartTokenAndIsDeletedFalse(String cartToken);
}
