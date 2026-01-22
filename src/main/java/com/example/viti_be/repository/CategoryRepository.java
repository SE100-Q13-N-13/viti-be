package com.example.viti_be.repository;

import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByName(String name);
    List<Category> findByParentId(UUID parentId);
    
    @Query("SELECT DISTINCT c FROM Category c " +
           "LEFT JOIN FETCH c.specs s " +
           "LEFT JOIN FETCH c.parent " +
           "WHERE c.isDeleted = false " +
           "AND (s IS NULL OR s.isDeleted = false OR s.isDeleted IS NULL)")
    List<Category> findAllByIsDeletedFalse();
    
    Optional<Category> findByIdAndIsDeletedFalse(UUID id);
}
