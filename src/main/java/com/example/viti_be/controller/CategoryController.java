package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CategoryRequest;
import com.example.viti_be.dto.request.CategorySpecRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.CategoryResponse;
import com.example.viti_be.dto.response.CategorySpecResponse;
import com.example.viti_be.mapper.CategoryMapper;
import com.example.viti_be.model.Category;
import com.example.viti_be.model.CategorySpec;
import com.example.viti_be.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        List<Category> entities = categoryService.getAllCategories();
        // Convert Entity List -> DTO List
        List<CategoryResponse> response = categoryMapper.toCategoryResponseList(entities);
        return ResponseEntity.ok(ApiResponse.success(response, "Success"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Tạo danh mục mới",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = {
                                    @Encoding(name = "data", contentType = "application/json")
                            }
                    )
            )
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @RequestPart("data") CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Category entity = categoryService.createCategory(request, image);
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toCategoryResponse(entity), "Created"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @RequestPart("data") CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Category entity = categoryService.updateCategory(id, request, image);
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toCategoryResponse(entity), "Updated"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    // --- SPECS ---

    @GetMapping("/{id}/specs")
    public ResponseEntity<ApiResponse<List<CategorySpecResponse>>> getSpecs(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "true") Boolean includeInherited) {
        
        if (includeInherited) {
            List<CategorySpec> specs = categoryService.getAllSpecsWithInheritance(id);
            return ResponseEntity.ok(ApiResponse.success(
                categoryMapper.toSpecResponseListWithInheritance(specs, id), 
                "Success"
            ));
        } else {
            List<CategorySpec> specs = categoryService.getSpecsByCategory(id);
            return ResponseEntity.ok(ApiResponse.success(
                categoryMapper.toSpecResponseList(specs), 
                "Success"
            ));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/specs")
    public ResponseEntity<ApiResponse<CategorySpecResponse>> addSpec(@RequestBody CategorySpecRequest request) {
        CategorySpec spec = categoryService.addSpecToCategory(request);
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toSpecResponse(spec), "Added spec"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/specs/{specId}")
    public ResponseEntity<ApiResponse<CategorySpecResponse>> updateSpec(@PathVariable UUID specId, @RequestBody CategorySpecRequest request) {
        CategorySpec spec = categoryService.updateSpec(specId, request);
        return ResponseEntity.ok(ApiResponse.success(categoryMapper.toSpecResponse(spec), "Updated spec"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/specs/{specId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpec(
            @PathVariable UUID specId,
            @RequestParam UUID categoryId) {
        categoryService.deleteSpec(specId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted spec"));
    }
}