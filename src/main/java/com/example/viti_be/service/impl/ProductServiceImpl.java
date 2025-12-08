package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.model.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.CloudinaryService;
import com.example.viti_be.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    ProductRepository productRepository;
    @Autowired
    ProductVariantRepository variantRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    SupplierRepository supplierRepository;
    @Autowired
    CloudinaryService cloudinaryService;

    @Override
    public Product createProduct(ProductRequest request, MultipartFile image) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        Product product = new Product();
        product.setName(request.getName());
        product.setCategory(category);
        product.setSupplier(supplier);
        product.setDescription(request.getDescription());
        product.setMinStockThreshold(request.getMinStockThreshold());
        product.setWarrantyPeriod(request.getWarrantyPeriod());
        product.setCommonSpecs(request.getCommonSpecs());
        product.setStatus("ACTIVE");

        if (image != null && !image.isEmpty()) {
            String url = cloudinaryService.uploadFile(image);
            product.setImageUrl(url);
        }

        return productRepository.save(product);
    }

    @Override
    public ProductVariant createVariant(VariantRequest request) {
        if (variantRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("SKU already exists: " + request.getSku());
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(request.getSku());
        variant.setVariantName(request.getVariantName());
        variant.setVariantSpecs(request.getVariantSpecs());
        variant.setSellingPrice(request.getSellingPrice());

        // purchasePriceAvg sẽ được update khi nhập kho (PO), khởi tạo = 0
        variant.setPurchasePriceAvg(java.math.BigDecimal.ZERO);

        return variantRepository.save(variant);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAllByIsDeletedFalse();
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found or has been deleted"));
    }

    @Override
    public Product updateProduct(UUID id, ProductRequest request, MultipartFile image) {
        Product product = getProductById(id);

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getMinStockThreshold() != null) product.setMinStockThreshold(request.getMinStockThreshold());
        if (request.getWarrantyPeriod() != null) product.setWarrantyPeriod(request.getWarrantyPeriod());
        if (request.getCommonSpecs() != null) product.setCommonSpecs(request.getCommonSpecs());

        if (request.getCategoryId() != null) {
            Category cat = categoryRepository.findById(request.getCategoryId()).orElseThrow();
            product.setCategory(cat);
        }
        if (request.getSupplierId() != null) {
            Supplier sup = supplierRepository.findById(request.getSupplierId()).orElseThrow();
            product.setSupplier(sup);
        }

        if (image != null && !image.isEmpty()) {
            String url = cloudinaryService.uploadFile(image);
            product.setImageUrl(url);
        }
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Override
    public ProductVariant updateVariant(UUID id, VariantRequest request) {
        ProductVariant variant = variantRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        if (request.getSku() != null) variant.setSku(request.getSku());
        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getSellingPrice() != null) variant.setSellingPrice(request.getSellingPrice());
        if (request.getVariantSpecs() != null) variant.setVariantSpecs(request.getVariantSpecs());

        return variantRepository.save(variant);
    }

    @Override
    public void deleteVariant(UUID id) {
        ProductVariant variant = variantRepository.findById(id).orElseThrow();
        variant.setIsDeleted(true);
        variantRepository.save(variant);
    }
}