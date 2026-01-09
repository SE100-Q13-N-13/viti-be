package com.example.viti_be.repository;

import com.example.viti_be.model.ProductSerial;
import com.example.viti_be.model.model_enum.ProductSerialStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductSerialRepository extends JpaRepository<ProductSerial, UUID> {
    
    Optional<ProductSerial> findBySerialNumber(String serialNumber);
    
    boolean existsBySerialNumber(String serialNumber);
    
    List<ProductSerial> findByProductVariantId(UUID productVariantId);
    
    List<ProductSerial> findByPurchaseOrderId(UUID purchaseOrderId);
    
    List<ProductSerial> findByStatus(ProductSerialStatus status);
    
    List<ProductSerial> findByProductVariantIdAndStatus(UUID productVariantId, ProductSerialStatus status);
    
    long countByProductVariantIdAndStatus(UUID productVariantId, ProductSerialStatus status);

    @Query("SELECT ps FROM ProductSerial ps WHERE ps.productVariant.id = :variantId " +
            "AND ps.status = 'AVAILABLE' ORDER BY ps.createdAt ASC")
    List<ProductSerial> findAvailableByVariantId(@Param("variantId") UUID variantId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ProductSerial ps WHERE ps.serialNumber = :serialNumber")
    Optional<ProductSerial> findBySerialNumberWithLock(@Param("serialNumber") String serialNumber);
}

