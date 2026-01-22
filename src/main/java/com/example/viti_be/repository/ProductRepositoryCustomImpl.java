package com.example.viti_be.repository;

import com.example.viti_be.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom implementation for dynamic product filtering
 * Builds native PostgreSQL query với JSONB operators
 */
@Slf4j
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Product> findWithDynamicFilters(
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String variantName,
            Map<String, String> specFilters,
            Pageable pageable) {

        // Build WHERE conditions
        List<String> conditions = new ArrayList<>();
        conditions.add("p.is_deleted = false");
        conditions.add("p.status = 'ACTIVE'");

        // Category filter
        if (categoryId != null) {
            conditions.add("p.category_id = :categoryId");
        }

        // Supplier filter
        if (supplierId != null) {
            conditions.add("p.supplier_id = :supplierId");
        }

        // Price filters (need at least 1 variant matching)
        boolean hasVariantFilters = false;
        if (minPrice != null) {
            conditions.add("v.selling_price >= :minPrice");
            hasVariantFilters = true;
        }
        if (maxPrice != null) {
            conditions.add("v.selling_price <= :maxPrice");
            hasVariantFilters = true;
        }

        // Variant name filter
        if (variantName != null && !variantName.isBlank()) {
            conditions.add("LOWER(v.variant_name) LIKE LOWER(:variantName)");
            hasVariantFilters = true;
        }

        // Dynamic spec filters
        if (specFilters != null && !specFilters.isEmpty()) {
            for (String specKey : specFilters.keySet()) {
                conditions.add(String.format(
                        "(v.variant_specs::jsonb->>'%s' = :%s OR v.variant_specs LIKE :%s_like)",
                        specKey, specKey, specKey
                ));
                hasVariantFilters = true;
            }
        }

        // Join variant only if needed
        String joinClause = hasVariantFilters
                ? "LEFT JOIN product_variants v ON v.product_id = p.id AND v.is_deleted = false"
                : "";

        // Build main query
        String sql = String.format("""
            SELECT DISTINCT p.*
            FROM products p
            %s
            WHERE %s
            """,
                joinClause,
                String.join(" AND ", conditions)
        );

        // Build count query
        String countSql = String.format("""
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            %s
            WHERE %s
            """,
                joinClause,
                String.join(" AND ", conditions)
        );

        // Create queries
        Query query = entityManager.createNativeQuery(sql, Product.class);
        Query countQuery = entityManager.createNativeQuery(countSql);

        // Set parameters
        setParameters(query, categoryId, supplierId, minPrice, maxPrice, variantName, specFilters);
        setParameters(countQuery, categoryId, supplierId, minPrice, maxPrice, variantName, specFilters);

        // Apply pagination
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        // Execute
        @SuppressWarnings("unchecked")
        List<Product> products = query.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        log.debug("Dynamic filter query returned {} products out of {} total", products.size(), total);

        return new PageImpl<>(products, pageable, total);
    }

    /**
     * Set parameters for query
     */
    private void setParameters(
            Query query,
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String variantName,
            Map<String, String> specFilters) {

        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (supplierId != null) {
            query.setParameter("supplierId", supplierId);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        if (variantName != null && !variantName.isBlank()) {
            query.setParameter("variantName", "%" + variantName + "%");
        }

        // Set spec filter parameters
        if (specFilters != null) {
            for (Map.Entry<String, String> entry : specFilters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                query.setParameter(key, value);
                query.setParameter(key + "_like", "%\"" + key + "\":\"" + value + "\"%");
            }
        }
    }
}