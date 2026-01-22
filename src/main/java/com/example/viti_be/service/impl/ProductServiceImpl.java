package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.dto.response.ProductResponse;
import com.example.viti_be.dto.response.ProductVariantResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.mapper.ProductMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.CloudinaryService;
import com.example.viti_be.service.ProductService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
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
    @Autowired
    CategorySpecRepository categorySpecRepository;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired ProductMapper productMapper;

    @Override
    public Product createProduct(ProductRequest request, MultipartFile image) {
        log.info(String.valueOf(request));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        validateSpecs(category.getId(), request.getCommonSpecs(), false);

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

        validateSpecs(product.getCategory().getId(), request.getVariantSpecs(), true);

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
    public PageResponse<ProductResponse> getAllProducts(
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String variantName,
            String variantSpec,
            String search,
            Pageable pageable
    ) {
        Page<Product> productPage = productRepository.findAllWithFilters(categoryId, supplierId, minPrice, maxPrice,  variantName, variantSpec, search, pageable);
        return PageResponse.from(productPage, productMapper::toProductResponse);
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Product not found or has been deleted"));
    }

    @Override
    public List<ProductVariant> getVariantsByProductId(UUID productId) {
        // Verify product exists
        Product product = getProductById(productId);
        return variantRepository.findByProductIdAndIsDeletedFalse(productId);
    }

    @Override
    public PageResponse<ProductVariantResponse> getAllVariants(Pageable pageable) {
        Page<ProductVariant> variantPage = variantRepository.findAllByIsDeletedFalse(pageable);
        return PageResponse.from(variantPage, productMapper::toVariantResponse);
    }

    @Override
    public PageResponse<ProductVariantResponse> getVariantsByCategory(UUID categoryId, Pageable pageable) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Page<ProductVariant> variantPage = variantRepository.findByCategoryId(categoryId, pageable);
        return PageResponse.from(variantPage, productMapper::toVariantResponse);
    }

    @Override
    public PageResponse<ProductVariantResponse> getVariantsByProduct(UUID productId, Pageable pageable) {
        // Verify product exists
        Product product = getProductById(productId);

        Page<ProductVariant> variantPage = variantRepository.findByProductIdAndIsDeletedFalse(productId, pageable);
        return PageResponse.from(variantPage, productMapper::toVariantResponse);
    }

    @Override
    public Product updateProduct(UUID id, ProductRequest request, MultipartFile image) {
        Product product = getProductById(id);
        validateSpecs(product.getCategory().getId(), request.getCommonSpecs(), true);

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

        Product product = productRepository.findById(variant.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        validateSpecs(product.getCategory().getId(), request.getVariantSpecs(), true);

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

    /**
     * Validate JSON specs dựa trên cấu hình Category
     * @param categoryId ID của danh mục
     * @param specsJson Chuỗi JSON specs từ request
     * @param isVariantSpec true nếu đang check Variant, false nếu check Product
     */
    private void validateSpecs(UUID categoryId, String specsJson, boolean isVariantSpec) {
        // 1. Lấy tất cả specs đã cấu hình cho Category này
        List<CategorySpec> definedSpecs = categorySpecRepository.findByCategoryIdAndIsDeletedFalse(categoryId);

        // 2. Lọc ra danh sách cần kiểm tra (Chung hoặc Riêng)
        List<CategorySpec> relevantSpecs = definedSpecs.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsVariantSpec()) == isVariantSpec)
                .toList();

        if (relevantSpecs.isEmpty()) return; // Không có quy định gì thì bỏ qua

        Map<String, Object> inputSpecs;
        try {
            // Parse JSON đầu vào hoặc tạo Map rỗng nếu null
            if (specsJson == null || specsJson.isBlank()) {
                inputSpecs = new HashMap<>();
            } else {
                inputSpecs = objectMapper.readValue(specsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON format for specs");
        }

        // 3. Duyệt qua từng quy định để kiểm tra
        for (CategorySpec spec : relevantSpecs) {
            String key = spec.getSpecKey();

            // Rule 1: Kiểm tra bắt buộc (Required)
            if (Boolean.TRUE.equals(spec.getIsRequired()) && !inputSpecs.containsKey(key)) {
                throw new RuntimeException("Missing required spec: " + spec.getSpecName() + " (" + key + ")");
            }

            // Rule 2: Kiểm tra giá trị hợp lệ nếu là dạng SELECT (Dropdown)
            if (inputSpecs.containsKey(key) && "SELECT".equalsIgnoreCase(spec.getDataType())) {
                String inputValue = String.valueOf(inputSpecs.get(key));
                try {
                    // spec.getOptions() là JSON mảng: ["Đỏ", "Xanh"]
                    List<String> allowedOptions = objectMapper.readValue((JsonParser) spec.getOptions(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});

                    if (!allowedOptions.contains(inputValue)) {
                        throw new RuntimeException("Invalid value for " + spec.getSpecName() + ". Allowed: " + allowedOptions);
                    }
                } catch (Exception e) {
                    log.error("Error parsing options for spec: {}", spec.getSpecKey(), e);
                }
            }
        }
    }
}