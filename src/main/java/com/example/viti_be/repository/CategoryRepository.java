package com.example.viti_be.repository;

import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByName(String name);
    List<Category> findByParent(UUID categoryId);
    List<Category> findAllByIsDeletedFalse();
    Optional<Category> findByIdAndIsDeletedFalse(UUID id);
}
