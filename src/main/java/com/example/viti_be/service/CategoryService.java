package com.example.viti_be.service;

import com.example.viti_be.dto.request.CategoryRequest;
import com.example.viti_be.dto.request.CategorySpecRequest;
import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    // Category CRUD
    Category createCategory(CategoryRequest request);
    Category updateCategory(UUID id, CategoryRequest request);
    void deleteCategory(UUID id);
    List<Category> getAllCategories();
    Category getCategoryById(UUID id);

    // Spec CRUD
    CategorySpec addSpecToCategory(CategorySpecRequest request);
    CategorySpec updateSpec(UUID specId, CategorySpecRequest request);
    void deleteSpec(UUID specId);
    List<CategorySpec> getSpecsByCategory(UUID categoryId);
}