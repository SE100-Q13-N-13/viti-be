package com.example.viti_be.repository;

import com.example.viti_be.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySku(String sku);
    Optional<ProductVariant> findByIdAndIsDeletedFalse(UUID id);
}