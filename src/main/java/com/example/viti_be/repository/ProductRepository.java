package com.example.viti_be.repository;

import com.example.viti_be.model.AuditLog;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByName(String name);
    Page<Product> findAllByIsDeletedFalse(Pageable pageable);
    Optional<Product> findByIdAndIsDeletedFalse(UUID id);

    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN p.variants v
    WHERE p.isDeleted = false
      AND p.status = 'ACTIVE'
      
      AND (:#{#search == null || #search.isBlank()} = true
                             OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
                             
      AND (:#{#categoryId == null} = true
           OR p.category.id = :categoryId)

      AND (:#{#supplierId == null} = true
           OR p.supplier.id = :supplierId)

      AND (:#{#minPrice == null} = true
           OR v.sellingPrice >= :minPrice)

      AND (:#{#maxPrice == null} = true
           OR v.sellingPrice <= :maxPrice)
           
      AND (:#{#variantName == null || #variantName.isBlank()} = true
                             OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :variantName, '%')))
            
                        AND (:#{#variantSpec == null || #variantSpec.isBlank()} = true
                             OR LOWER(v.variantSpecs) LIKE LOWER(CONCAT('%', :variantSpec, '%')))
""")
    Page<Product> findAllWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("supplierId") UUID supplierId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("variantName") String variantName,
            @Param("variantSpec") String variantSpec,
            @Param("search") String search,
            Pageable pageable
    );
}
