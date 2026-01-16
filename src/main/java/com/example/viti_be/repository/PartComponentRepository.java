package com.example.viti_be.repository;

import com.example.viti_be.model.PartComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartComponentRepository extends JpaRepository<PartComponent, UUID> {

    Optional<PartComponent> findByIdAndIsDeletedFalse(UUID id);

    List<PartComponent> findByIsDeletedFalseOrderByName();

    List<PartComponent> findAllByIsDeletedFalseOrderByName();


    List<PartComponent> findByPartTypeAndIsDeletedFalse(String partType);

    // Find low stock parts
    @Query("SELECT pc FROM PartComponent pc JOIN Inventory i ON i.partComponentId = pc.id " +
            "WHERE i.quantityAvailable <= pc.minStock AND pc.isDeleted = false")
    List<PartComponent> findLowStockParts();
}