package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CategoryRequest;
import com.example.viti_be.dto.request.CategorySpecRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import com.example.viti_be.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories(), "Success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.createCategory(request), "Created"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable UUID id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(id, request), "Updated"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    @GetMapping("/{id}/specs")
    public ResponseEntity<ApiResponse<List<CategorySpec>>> getSpecs(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getSpecsByCategory(id), "Success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/specs")
    public ResponseEntity<ApiResponse<CategorySpec>> addSpec(@RequestBody CategorySpecRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.addSpecToCategory(request), "Added spec"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/specs/{specId}")
    public ResponseEntity<ApiResponse<CategorySpec>> updateSpec(@PathVariable UUID specId, @RequestBody CategorySpecRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateSpec(specId, request), "Updated spec"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/specs/{specId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpec(@PathVariable UUID specId) {
        categoryService.deleteSpec(specId);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted spec"));
    }
}