package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.dto.response.ProductResponse;
import com.example.viti_be.dto.response.ProductVariantResponse;
import com.example.viti_be.dto.response.VariantFilterOptionsResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.mapper.ProductMapper;
import com.example.viti_be.model.*;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.CloudinaryService;
import com.example.viti_be.service.ProductService;
import com.example.viti_be.specification.ProductSpecification;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

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
            String searchKeyword,
            Map<String, String> variantSpecs,
            Pageable pageable
    ) {
        // Execute query
        Page<Product> productPage = productRepository.findWithDynamicFilters(
                categoryId,
                supplierId,
                minPrice,
                maxPrice,
                searchKeyword,
                variantSpecs,
                pageable
        );

        // Check if we need to filter variants in response
        boolean hasVariantFilter = (variantSpecs != null && !variantSpecs.isEmpty())
                || minPrice != null
                || maxPrice != null;

        // Convert to response
        List<ProductResponse> productResponses = productPage.getContent().stream()
                .map(product -> {
                    ProductResponse response = productMapper.toProductResponse(product);

                    // Filter variants if filter params exist
                    if (hasVariantFilter && response.getVariants() != null) {
                        List<ProductVariantResponse> filteredVariants = response.getVariants().stream()
                                .filter(variant -> matchesFilters(variant, variantSpecs, minPrice, maxPrice))
                                .toList();
                        response.setVariants(filteredVariants);
                    }

                    return response;
                })
                .toList();

        return new PageResponse<>(
                productResponses,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.isLast(),
                productPage.isFirst(),
                productResponses.size()
        );
    }

    /**
     * Check if a variant matches the filter criteria
     */
    private boolean matchesFilters(
            ProductVariantResponse variant,
            Map<String, String> specFilters,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        // Price filters
        if (minPrice != null && variant.getSellingPrice().compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && variant.getSellingPrice().compareTo(maxPrice) > 0) {
            return false;
        }

        // Spec filters
        if (specFilters != null && !specFilters.isEmpty()) {
            try {
                Map<String, Object> variantSpecs = objectMapper.readValue(
                        variant.getVariantSpecs(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );

                for (Map.Entry<String, String> filter : specFilters.entrySet()) {
                    String key = filter.getKey().toLowerCase();
                    String expectedValue = filter.getValue();

                    // Case-insensitive key matching
                    String actualValue = variantSpecs.entrySet().stream()
                            .filter(e -> e.getKey().toLowerCase().equals(key))
                            .map(e -> String.valueOf(e.getValue()))
                            .findFirst()
                            .orElse(null);

                    if (!expectedValue.equals(actualValue)) {
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse variant specs for filtering: {}", e.getMessage());
                return false;
            }
        }

        return true;
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

    @Override
    public VariantFilterOptionsResponse getVariantFilterOptions(UUID categoryId) {
        // 1. Lấy danh sách CategorySpec (variant specs)
        List<CategorySpec> variantSpecs;
        if (categoryId != null) {
            variantSpecs = categorySpecRepository
                    .findByCategoryIdAndIsVariantSpecTrueAndIsDeletedFalse(categoryId);
        } else {
            variantSpecs = categorySpecRepository
                    .findByIsVariantSpecTrueAndIsDeletedFalse();
        }

        // 2. Lấy tất cả variants
        List<ProductVariant> variants;
        if (categoryId != null) {
            variants = variantRepository.findAllByCategoryId(categoryId);
        } else {
            variants = variantRepository.findAllByIsDeletedFalse();
        }

        // 3. Tạo Map để lưu filter options (case-insensitive keys)
        Map<String, Set<String>> filterMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, VariantFilterOptionsResponse.SpecMetadata> metadataMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // 4. Khởi tạo từ CategorySpec.options (nếu có)
        for (CategorySpec spec : variantSpecs) {
            String key = normalizeKey(spec.getSpecKey());

            // Thêm metadata (dùng key chuẩn hóa)
            metadataMap.put(key, new VariantFilterOptionsResponse.SpecMetadata(
                    spec.getSpecName(),
                    spec.getDataType(),
                    spec.getIsRequired()
            ));

            // Nếu có options được định nghĩa sẵn, dùng luôn
            if (spec.getOptions() != null && !spec.getOptions().isEmpty()) {
                filterMap.put(key, new LinkedHashSet<>(spec.getOptions()));
            } else {
                filterMap.put(key, new LinkedHashSet<>());
            }
        }

        // 5. Parse variants để lấy giá trị thực tế
        for (ProductVariant variant : variants) {
            if (variant.getVariantSpecs() == null || variant.getVariantSpecs().isBlank()) {
                continue;
            }

            try {
                Map<String, Object> specs = objectMapper.readValue(
                        variant.getVariantSpecs(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );

                // Merge vào filterMap
                for (Map.Entry<String, Object> entry : specs.entrySet()) {
                    String key = normalizeKey(entry.getKey());
                    String value = String.valueOf(entry.getValue());

                    filterMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(value);

                    // Nếu spec này chưa có metadata, tạo một cái basic
                    if (!metadataMap.containsKey(key)) {
                        metadataMap.put(key, new VariantFilterOptionsResponse.SpecMetadata(
                                key, // Dùng key chuẩn hóa làm name
                                "TEXT",
                                false
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse variantSpecs for variant {}: {}",
                        variant.getId(), e.getMessage());
            }
        }

        // 6. Convert Set -> List và sort với Natural Order Comparator
        Map<String, List<String>> resultFilters = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : filterMap.entrySet()) {
            List<String> values = new ArrayList<>(entry.getValue());
            values.sort(new NaturalOrderComparator()); // Smart sort
            resultFilters.put(entry.getKey(), values);
        }

        return new VariantFilterOptionsResponse(resultFilters, metadataMap);
    }

    /**
     * Chuẩn hóa key để tránh trùng lặp (RAM vs ram -> ram)
     */
    private String normalizeKey(String key) {
        if (key == null) return null;
        // Convert sang lowercase và trim
        return key.trim().toLowerCase();
    }

    /**
     * Comparator thông minh để sort natural order
     * VD: "8GB" < "16GB" < "32GB" (không phải "16GB" < "32GB" < "8GB")
     */
    private static class NaturalOrderComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (s1 == null || s2 == null) {
                return s1 == null ? (s2 == null ? 0 : -1) : 1;
            }

            int len1 = s1.length();
            int len2 = s2.length();
            int i = 0, j = 0;

            while (i < len1 && j < len2) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(j);

                // Nếu cả 2 đều là số, so sánh theo numeric
                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    // Lấy toàn bộ số
                    StringBuilder num1 = new StringBuilder();
                    StringBuilder num2 = new StringBuilder();

                    while (i < len1 && Character.isDigit(s1.charAt(i))) {
                        num1.append(s1.charAt(i++));
                    }
                    while (j < len2 && Character.isDigit(s2.charAt(j))) {
                        num2.append(s2.charAt(j++));
                    }

                    // So sánh numeric
                    try {
                        int n1 = Integer.parseInt(num1.toString());
                        int n2 = Integer.parseInt(num2.toString());
                        if (n1 != n2) {
                            return Integer.compare(n1, n2);
                        }
                    } catch (NumberFormatException e) {
                        // Fallback to string comparison
                        int cmp = num1.toString().compareTo(num2.toString());
                        if (cmp != 0) return cmp;
                    }
                } else {
                    // So sánh ký tự thường (case-insensitive)
                    int cmp = Character.compare(
                            Character.toLowerCase(c1),
                            Character.toLowerCase(c2)
                    );
                    if (cmp != 0) return cmp;
                    i++;
                    j++;
                }
            }

            // Nếu một chuỗi hết trước -> chuỗi ngắn hơn đứng trước
            return Integer.compare(len1, len2);
        }
    }
}