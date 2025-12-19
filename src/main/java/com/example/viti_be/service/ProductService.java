package com.example.viti_be.service;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    Product createProduct(ProductRequest request, MultipartFile image);
    ProductVariant createVariant(VariantRequest request);
    List<Product> getAllProducts();
    Product getProductById(UUID id);
    Product updateProduct(UUID id, ProductRequest request, MultipartFile image);
    void deleteProduct(UUID id);
    ProductVariant updateVariant(UUID id, VariantRequest request);
    void deleteVariant(UUID id);
}