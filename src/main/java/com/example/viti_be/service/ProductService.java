package com.example.viti_be.service;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.dto.response.ProductResponse;
import com.example.viti_be.dto.response.ProductVariantResponse;
import com.example.viti_be.dto.response.VariantFilterOptionsResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductService {
    Product createProduct(ProductRequest request, MultipartFile image);
    ProductVariant createVariant(VariantRequest request);

    /**
     * Lấy danh sách products với dynamic filters
     * @param categoryId Filter theo category (optional)
     * @param supplierId Filter theo supplier (optional)
     * @param minPrice Giá tối thiểu (optional)
     * @param maxPrice Giá tối đa (optional)
     * @param searchKeyword Từ khóa tìm kiếm trong tên sản phẩm (optional)
     * @param variantSpecs Dynamic specs filter - Map<specKey, value> (optional)
     *                     VD: {"color": "Đen", "ram": "16GB"}
     * @param pageable Pagination
     */
    PageResponse<ProductResponse> getAllProducts(
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String searchKeyword,
            Map<String, List<String>> variantSpecs,
            Pageable pageable
    );

    Product getProductById(UUID id);

    List<ProductVariant> getVariantsByProductId(UUID productId);

    PageResponse<ProductVariantResponse> getAllVariants(Pageable pageable);

    PageResponse<ProductVariantResponse> getVariantsByCategory(UUID categoryId, Pageable pageable);

    PageResponse<ProductVariantResponse> getVariantsByProduct(UUID productId, Pageable pageable);

    Product updateProduct(UUID id, ProductRequest request, MultipartFile image);
    void deleteProduct(UUID id);
    ProductVariant updateVariant(UUID id, VariantRequest request);
    void deleteVariant(UUID id);

    VariantFilterOptionsResponse getVariantFilterOptions(UUID categoryId);
}