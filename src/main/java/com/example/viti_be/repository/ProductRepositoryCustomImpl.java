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
            String searchKeyword,
            Map<String, List<String>> specFilters,
            Pageable pageable) {

        List<String> conditions = new ArrayList<>();
        conditions.add("p.is_deleted = false");
        conditions.add("p.status = 'ACTIVE'");

        if (searchKeyword != null && !searchKeyword.isBlank()) {
            conditions.add("LOWER(p.name) LIKE LOWER(:searchKeyword)");
        }
        if (categoryId != null) {
            conditions.add("p.category_id = :categoryId");
        }
        if (supplierId != null) {
            conditions.add("p.supplier_id = :supplierId");
        }

        boolean hasVariantFilters = false;

        if (minPrice != null) {
            conditions.add("v.selling_price >= :minPrice");
            hasVariantFilters = true;
        }
        if (maxPrice != null) {
            conditions.add("v.selling_price <= :maxPrice");
            hasVariantFilters = true;
        }

        // Dynamic spec filters với logic AND giữa specs, OR trong values
        if (specFilters != null && !specFilters.isEmpty()) {
            int specIndex = 0;
            for (Map.Entry<String, List<String>> entry : specFilters.entrySet()) {
                String specKey = entry.getKey();
                List<String> values = entry.getValue();

                if (values.isEmpty()) continue;

                if (values.size() == 1) {
                    // Single value: exact match
                    conditions.add(String.format(
                            "(v.variant_specs::jsonb->>'%s' = :spec_%d_val_0 OR v.variant_specs LIKE :spec_%d_like_0)",
                            specKey, specIndex, specIndex
                    ));
                } else {
                    // Multiple values: OR within same spec
                    List<String> orConditions = new ArrayList<>();
                    for (int i = 0; i < values.size(); i++) {
                        orConditions.add(String.format(
                                "(v.variant_specs::jsonb->>'%s' = :spec_%d_val_%d OR v.variant_specs LIKE :spec_%d_like_%d)",
                                specKey, specIndex, i, specIndex, i
                        ));
                    }
                    conditions.add("(" + String.join(" OR ", orConditions) + ")");
                }

                specIndex++;
                hasVariantFilters = true;
            }
        }

        String joinClause = hasVariantFilters
                ? "LEFT JOIN product_variants v ON v.product_id = p.id AND v.is_deleted = false"
                : "";

        String sql = String.format("""
        SELECT DISTINCT p.*
        FROM products p
        %s
        WHERE %s
        """, joinClause, String.join(" AND ", conditions));

        String countSql = String.format("""
        SELECT COUNT(DISTINCT p.id)
        FROM products p
        %s
        WHERE %s
        """, joinClause, String.join(" AND ", conditions));

        log.debug("Generated SQL: {}", sql);

        Query query = entityManager.createNativeQuery(sql, Product.class);
        Query countQuery = entityManager.createNativeQuery(countSql);

        setParameters(query, categoryId, supplierId, minPrice, maxPrice, searchKeyword, specFilters);
        setParameters(countQuery, categoryId, supplierId, minPrice, maxPrice, searchKeyword, specFilters);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Product> products = query.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(products, pageable, total);
    }

    private void setParameters(
            Query query,
            UUID categoryId,
            UUID supplierId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String searchKeyword,
            Map<String, List<String>> specFilters) {
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            query.setParameter("searchKeyword", "%" + searchKeyword + "%");
        }

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

        // Set multi-value spec parameters
        if (specFilters != null) {
            int specIndex = 0;
            for (Map.Entry<String, List<String>> entry : specFilters.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();

                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i);
                    query.setParameter(String.format("spec_%d_val_%d", specIndex, i), value);
                    query.setParameter(
                            String.format("spec_%d_like_%d", specIndex, i),
                            "%\"" + key + "\":\"" + value + "\"%"
                    );
                }
                specIndex++;
            }
        }
    }
}