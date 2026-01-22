package com.example.viti_be.repository;

import com.example.viti_be.model.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySku(String sku);
    Optional<ProductVariant> findByIdAndIsDeletedFalse(UUID id);
    // Lấy tất cả variants của 1 product
    List<ProductVariant> findByProductIdAndIsDeletedFalse(UUID productId);

    // Lấy tất cả variants (có pagination)
    Page<ProductVariant> findAllByIsDeletedFalse(Pageable pageable);

    // Lấy variants theo category (qua product)
    @Query("SELECT v FROM ProductVariant v WHERE v.product.category.id = :categoryId AND v.isDeleted = false")
    Page<ProductVariant> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    // Lấy variants theo product (có pagination)
    Page<ProductVariant> findByProductIdAndIsDeletedFalse(UUID productId, Pageable pageable);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.category.id = :categoryId AND v.isDeleted = false")
    List<ProductVariant> findAllByCategoryId(@Param("categoryId") UUID categoryId);

    // Lấy tất cả variants (không phân trang) - để aggregate filter options toàn hệ thống
    List<ProductVariant> findAllByIsDeletedFalse();
}