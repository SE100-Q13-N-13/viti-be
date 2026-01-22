package com.example.viti_be.specification;

import com.example.viti_be.model.Product;
import com.example.viti_be.model.ProductVariant;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dynamic Specification builder cho Product filtering
 * Support filter theo category, supplier, price range, và dynamic variant specs
 */
@Slf4j
public class ProductSpecification {

    /**
     * Build complete specification từ filter parameters
     */
    public static Specification<Product> buildSpecification(
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String searchKeyword,
            Map<String, String> variantSpecs // Dynamic specs: {"color": "Đen", "ram": "16GB"}
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Base filters
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // 2. Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // 3. Supplier filter
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }

            // 4. Search keyword (search in product name)
            if (searchKeyword != null && !searchKeyword.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + searchKeyword.toLowerCase() + "%"
                ));
            }

            // 5. Price range & variant specs - Join với variants
            if ((minPrice != null || maxPrice != null) ||
                    (variantSpecs != null && !variantSpecs.isEmpty())) {

                // Join với variants
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                predicates.add(cb.isFalse(variantJoin.get("isDeleted")));

                // Price range filter
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            variantJoin.get("sellingPrice"), minPrice
                    ));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            variantJoin.get("sellingPrice"), maxPrice
                    ));
                }

                // Dynamic variant specs filter
                if (variantSpecs != null && !variantSpecs.isEmpty()) {
                    for (Map.Entry<String, String> spec : variantSpecs.entrySet()) {
                        String key = spec.getKey();
                        String value = spec.getValue();

                        // Filter JSON: variantSpecs LIKE '%"key":"value"%'
                        // Ví dụ: variantSpecs LIKE '%"color":"Đen"%'
                        String pattern = String.format("%%\"%s\":\"%s\"%%", key, value);
                        predicates.add(cb.like(
                                cb.lower(variantJoin.get("variantSpecs")),
                                pattern.toLowerCase()
                        ));
                    }
                }
            }

            // DISTINCT để tránh duplicate khi join
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Simplified version - chỉ filter category
     */
    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) ->
                categoryId == null ? cb.conjunction() :
                        cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * Filter theo supplier
     */
    public static Specification<Product> hasSupplier(UUID supplierId) {
        return (root, query, cb) ->
                supplierId == null ? cb.conjunction() :
                        cb.equal(root.get("supplier").get("id"), supplierId);
    }

    /**
     * Filter active products only
     */
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("isDeleted")),
                cb.equal(root.get("status"), "ACTIVE")
        );
    }
}