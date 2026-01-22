package com.example.viti_be.repository;

import com.example.viti_be.model.PurchaseOrder;
import com.example.viti_be.model.model_enum.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    
    Optional<PurchaseOrder> findByIdAndIsDeletedFalse(UUID id);
    
    Page<PurchaseOrder> findAllByIsDeletedFalse(Pageable pageable);
    
    List<PurchaseOrder> findAllByStatusAndIsDeletedFalse(PurchaseOrderStatus status);
    
    List<PurchaseOrder> findAllBySupplierIdAndIsDeletedFalse(UUID supplierId);
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.poNumber = :poNumber AND po.isDeleted = false")
    Optional<PurchaseOrder> findByPoNumber(@Param("poNumber") String poNumber);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(po.poNumber, LENGTH(:prefix) + 1) AS integer)), 0) FROM PurchaseOrder po WHERE po.poNumber LIKE CONCAT(:prefix, '%')")
    Integer findMaxPoNumberByPrefix(@Param("prefix") String prefix);
    
    boolean existsByPoNumber(String poNumber);
}
