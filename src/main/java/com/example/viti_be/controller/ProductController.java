package com.example.viti_be.controller;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import com.example.viti_be.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Lấy danh sách sản phẩm (Public)
    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(), "Fetch products success"));
    }

    // Xem chi tiết
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id), "Fetch product success"));
    }

    // Tạo sản phẩm mới (Chỉ Admin/Kho)
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @RequestPart("data") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Product product = productService.createProduct(request, image);
        return ResponseEntity.ok(ApiResponse.success(product, "Product created successfully"));
    }

    // Tạo biến thể (SKU) cho sản phẩm
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/variants")
    public ResponseEntity<ApiResponse<ProductVariant>> createVariant(@RequestBody VariantRequest request) {
        ProductVariant variant = productService.createVariant(request);
        return ResponseEntity.ok(ApiResponse.success(variant, "Variant created successfully"));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable UUID id,
            @RequestPart("data") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request, image), "Updated"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariant>> updateVariant(@PathVariable UUID id, @RequestBody VariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateVariant(id, request), "Updated Variant"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable UUID id) {
        productService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted Variant"));
    }
}