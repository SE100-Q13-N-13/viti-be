package com.example.viti_be.repository;

import com.example.viti_be.model.ProductSerial;
import com.example.viti_be.model.model_enum.ProductSerialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
