package com.example.viti_be.controller;

import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.ProductVariantResponse;
import com.example.viti_be.dto.response.VariantFilterOptionsResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/variants")
@Tag(name = "Product Variant Management", description = "APIs for managing product variants")
@RequiredArgsConstructor
public class VariantController {

    private final ProductService productService;

    /**
     * GET /api/variants - Lấy tất cả variants (Public, có pagination)
     * Hỗ trợ filter theo:
     * - categoryId (optional)
     * - productId (optional)
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách variants với filter (Public)",
            description = "Dùng để FE tạo trang filter sản phẩm theo category, price range...")
    public ResponseEntity<ApiResponse<PageResponse<ProductVariantResponse>>> getVariants(
            @Parameter(description = "Filter theo category ID")
            @RequestParam(required = false) UUID categoryId,

            @Parameter(description = "Filter theo product ID")
            @RequestParam(required = false) UUID productId,

            @ParameterObject Pageable pageable) {

        PageResponse<ProductVariantResponse> variants;

        // Priority: productId > categoryId > all
        if (productId != null) {
            variants = productService.getVariantsByProduct(productId, pageable);
        } else if (categoryId != null) {
            variants = productService.getVariantsByCategory(categoryId, pageable);
        } else {
            variants = productService.getAllVariants(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(variants, "Fetch variants success"));
    }

    /**
     * GET /api/variants/filter-options - Lấy tất cả filter options (Public)
     * Tổng hợp tất cả giá trị unique của variant specs để tạo bộ filter động
     *
     * @param categoryId Optional - Chỉ lấy filter options của category này
     * @return Map<specKey, List<uniqueValues>>
     */
    @GetMapping("/filter-options")
    @Operation(summary = "Lấy tất cả filter options cho variants (Public)",
            description = "Tổng hợp tất cả giá trị unique của variant specs. VD: color: [Đỏ, Xanh], ram: [8GB, 16GB]")
    public ResponseEntity<ApiResponse<VariantFilterOptionsResponse>> getFilterOptions(
            @Parameter(description = "Chỉ lấy options của category này (optional)")
            @RequestParam(required = false) UUID categoryId) {

        VariantFilterOptionsResponse options = productService.getVariantFilterOptions(categoryId);
        return ResponseEntity.ok(ApiResponse.success(options, "Fetch filter options success"));
    }
}