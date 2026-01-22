package com.example.viti_be.repository;

import com.example.viti_be.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Custom repository interface for dynamic product filtering
 */
public interface ProductRepositoryCustom {

    /**
     * Find products with dynamic spec filters
     *
     * @param categoryId Filter by category
     * @param supplierId Filter by supplier
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @param variantName Search in variant name
     * @param specFilters Dynamic spec filters (color=Đen, ram=16GB, etc.)
     * @param pageable Pagination
     * @return Page of products
     */
    Page<Product> findWithDynamicFilters(
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String variantName,
            Map<String, String> specFilters,
            Pageable pageable
    );
}