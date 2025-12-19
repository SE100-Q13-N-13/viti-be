package com.example.viti_be.repository;

import com.example.viti_be.model.CategorySpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface CategorySpecRepository extends JpaRepository<CategorySpec, UUID> {
    List<CategorySpec> findByCategoryId(UUID categoryId);
    List<CategorySpec> findByCategoryIdAndIsDeletedFalse(UUID categoryId);
    Optional<CategorySpec> findByIdAndIsDeletedFalse(UUID id);
}