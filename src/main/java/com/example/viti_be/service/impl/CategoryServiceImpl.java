package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.CategoryRequest;
import com.example.viti_be.dto.request.CategorySpecRequest;
import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import com.example.viti_be.repository.CategoryRepository;
import com.example.viti_be.repository.CategorySpecRepository;
import com.example.viti_be.service.CategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired CategoryRepository categoryRepository;
    @Autowired CategorySpecRepository categorySpecRepository;
    @Autowired ObjectMapper objectMapper;

    @Override
    public Category createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        }

        if (request.getRelatedCategoryIds() != null && !request.getRelatedCategoryIds().isEmpty()) {
            try {
                category.setRelatedCategoryIds(objectMapper.writeValueAsString(request.getRelatedCategoryIds()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting relatedCategoryIds to JSON");
            }
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(UUID id, CategoryRequest request) {
        Category category = getCategoryById(id);
        category.setName(request.getName());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            if (parent.getId().equals(id)) {
                throw new RuntimeException("Category cannot be parent of itself");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        if (request.getRelatedCategoryIds() != null) {
            try {
                category.setRelatedCategoryIds(objectMapper.writeValueAsString(request.getRelatedCategoryIds()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting relatedCategoryIds to JSON");
            }
        }
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(UUID id) {
        Category category = getCategoryById(id);
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByIsDeletedFalse();
    }

    @Override
    public Category getCategoryById(UUID id) {
        return categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public CategorySpec addSpecToCategory(CategorySpecRequest request) {
        Category category = getCategoryById(request.getCategoryId());
        CategorySpec spec = new CategorySpec();
        spec.setCategory(category);
        return mapRequestToSpec(request, spec);
    }

    @Override
    public CategorySpec updateSpec(UUID specId, CategorySpecRequest request) {
        CategorySpec spec = categorySpecRepository.findByIdAndIsDeletedFalse(specId)
                .orElseThrow(() -> new RuntimeException("Spec not found"));
        return mapRequestToSpec(request, spec);
    }

    private CategorySpec mapRequestToSpec(CategorySpecRequest request, CategorySpec spec) {
        spec.setSpecKey(request.getSpecKey());
        spec.setSpecName(request.getSpecName());
        spec.setIsRequired(request.getIsRequired());
        spec.setDataType(request.getDataType());
        if (request.getOptions() != null) {
            try {
                spec.setOptions(objectMapper.writeValueAsString(request.getOptions()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing options");
            }
        }
        return categorySpecRepository.save(spec);
    }

    @Override
    public void deleteSpec(UUID specId) {
        CategorySpec spec = categorySpecRepository.findByIdAndIsDeletedFalse(specId)
                .orElseThrow(() -> new RuntimeException("Spec not found"));
        spec.setIsDeleted(true);
        categorySpecRepository.save(spec);
    }

    @Override
    public List<CategorySpec> getSpecsByCategory(UUID categoryId) {
        return categorySpecRepository.findByCategoryIdAndIsDeletedFalse(categoryId);
    }
}