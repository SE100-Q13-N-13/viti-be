package com.example.viti_be.controller;

import com.example.viti_be.dto.request.ProductRequest;
import com.example.viti_be.dto.request.VariantRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.ProductResponse;
import com.example.viti_be.dto.response.ProductVariantResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.mapper.ProductMapper;
import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import com.example.viti_be.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "APIs for managing products and product variants")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper; // Inject Mapper

    private static final Set<String> RESERVED_PARAMS = Set.of(
            "categoryid", "supplierid", "minprice", "maxprice",
            "search", "page", "size", "sort"
    );

    /**
     * GET /api/products - Lấy danh sách sản phẩm với dynamic filters
     * Example requests:
     * - Basic: /api/products?page=0&size=20
     * - Filter category: /api/products?categoryId=xxx&page=0
     * - Filter specs: /api/products?color=Đen&ram=16GB&page=0
     * - Combined: /api/products?categoryId=xxx&color=Đen&minPrice=1000&page=0
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách sản phẩm với filters (Public)",
            description = """
            Support dynamic filtering:
            - categoryId: UUID của category
            - supplierId: UUID của supplier
            - minPrice, maxPrice: Lọc theo giá
            - search: Tìm kiếm trong tên sản phẩm
            - Dynamic specs: Bất kỳ spec nào từ /api/variants/filter-options
              VD: color=Đen, ram=16GB, storage=512GB SSD
            """
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Filter theo category ID")
            @RequestParam(required = false) UUID categoryId,

            @Parameter(description = "Filter theo supplier ID")
            @RequestParam(required = false) UUID supplierId,

            @Parameter(description = "Giá tối thiểu")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Giá tối đa")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Tìm kiếm trong tên sản phẩm")
            @RequestParam(required = false) String search,

            @Parameter(hidden = true) // Hide từ Swagger vì dynamic
            @RequestParam Map<String, String> allParams,

            @ParameterObject Pageable pageable
    ) {
        // Parse dynamic specs từ request params
        Map<String, List<String>> variantSpecs = parseDynamicSpecsMultiValue(allParams);

        log.info("📊 Product filter request - category: {}, supplier: {}, price: {}-{}, search: '{}', specs: {}",
                categoryId, supplierId, minPrice, maxPrice, search, variantSpecs);

        PageResponse<ProductResponse> products = productService.getAllProducts(
                categoryId,
                supplierId,
                minPrice,
                maxPrice,
                search,
                variantSpecs,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(products, "Fetch products success"));
    }

    /**
     * Parse dynamic specs từ request params
     * Lọc bỏ reserved params (categoryId, page, size...) và giữ lại specs
     */
    private Map<String, List<String>> parseDynamicSpecsMultiValue(Map<String, String> allParams) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        allParams.entrySet().stream()
                .filter(entry -> !RESERVED_PARAMS.contains(entry.getKey().toLowerCase()))
                .forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // Support both formats:
                    // ?color=Đỏ,Vàng  hoặc  ?color=Đỏ&color=Vàng
                    List<String> values = Arrays.asList(value.split(","));

                    result.merge(key, values, (oldList, newList) -> {
                        List<String> merged = new ArrayList<>(oldList);
                        merged.addAll(newList);
                        return merged;
                    });
                });

        return result;
    }

    // Xem chi tiết
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toProductResponse(product),
                "Fetch product success"
        ));
    }

    /**
     * GET /api/products/{productId}/variants - Lấy variants của 1 product (Public)
     */
    @GetMapping("/{productId}/variants")
    @Operation(summary = "Lấy tất cả variants của 1 sản phẩm (Public)",
            description = "Dùng để hiển thị các biến thể (màu sắc, dung lượng...) trên trang chi tiết")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getProductVariants(
            @PathVariable UUID productId) {
        List<ProductVariant> variants = productService.getVariantsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toVariantResponseList(variants),
                "Fetch variants success"
        ));
    }

    // Tạo sản phẩm mới (Chỉ Admin/Kho)
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Tạo sản phẩm mới",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = {
                                    @Encoding(name = "data", contentType = "application/json")
                            }
                    )
            )
    )
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestPart("data") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Product createdProduct = productService.createProduct(request, image);
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toProductResponse(createdProduct),
                "Product created successfully"
        ));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @RequestPart("data") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Product updatedProduct = productService.updateProduct(id, request, image);
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toProductResponse(updatedProduct),
                "Updated"
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    // --- VARIANT ENDPOINTS ---

    // Tạo biến thể (SKU) cho sản phẩm
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(@RequestBody VariantRequest request) {
        ProductVariant variant = productService.createVariant(request);
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toVariantResponse(variant),
                "Variant created successfully"
        ));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PutMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(@PathVariable UUID id, @RequestBody VariantRequest request) {
        ProductVariant updatedVariant = productService.updateVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                productMapper.toVariantResponse(updatedVariant),
                "Updated Variant"
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable UUID id) {
        productService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted Variant"));
    }
}