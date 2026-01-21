package com.example.viti_be.service;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.dto.response.ProductResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    Product createProduct(ProductRequest request, MultipartFile image);
    ProductVariant createVariant(VariantRequest request);
    PageResponse<ProductResponse> getAllProducts(UUID categoryId, UUID supplierId, BigDecimal minPrice, BigDecimal maxPrice, String variantName, String variantSpec, Pageable pageable);
    Product getProductById(UUID id);
    Product updateProduct(UUID id, ProductRequest request, MultipartFile image);
    void deleteProduct(UUID id);
    ProductVariant updateVariant(UUID id, VariantRequest request);
    void deleteVariant(UUID id);
}