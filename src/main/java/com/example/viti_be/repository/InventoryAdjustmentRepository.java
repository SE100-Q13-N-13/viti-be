package com.example.viti_be.repository;

import com.example.viti_be.model.InventoryAdjustment;
import com.example.viti_be.model.model_enum.InventoryAdjustmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {
    
    Optional<InventoryAdjustment> findByIdAndIsDeletedFalse(UUID id);
    
    List<InventoryAdjustment> findAllByIsDeletedFalse();
    
    List<InventoryAdjustment> findAllByStatusAndIsDeletedFalse(InventoryAdjustmentStatus status);
    
    Optional<InventoryAdjustment> findByReferenceCodeAndIsDeletedFalse(UUID referenceCode);
}
