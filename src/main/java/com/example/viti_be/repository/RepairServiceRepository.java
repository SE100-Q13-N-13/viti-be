package com.example.viti_be.repository;

import com.example.viti_be.model.RepairService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepairServiceRepository extends JpaRepository<RepairService, UUID> {

    Optional<RepairService> findByIdAndIsDeletedFalse(UUID id);

    List<RepairService> findByIsActiveTrueAndIsDeletedFalseOrderByName();

    Page<RepairService> findAllByIsDeletedFalseOrderByName(Pageable pageable);

    List<RepairService> findByCategoryAndIsActiveTrueAndIsDeletedFalse(String category);
}
