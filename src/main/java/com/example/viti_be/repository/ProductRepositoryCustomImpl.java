package com.example.viti_be.repository;

import com.example.viti_be.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom implementation với PostgreSQL Full-Text Search
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

        boolean useFullTextSearch = searchKeyword != null && !searchKeyword.isBlank();

        // Full-Text Search với ranking
        if (useFullTextSearch) {
            // Combine FTS và LIKE fallback trong 1 condition
            conditions.add("""
                (
                    p.search_vector @@ plainto_tsquery('simple', :searchKeyword)
                    OR LOWER(c.name) LIKE LOWER(:searchKeywordLike)
                    OR LOWER(s.name) LIKE LOWER(:searchKeywordLike)
                    OR LOWER(v.variant_name) LIKE LOWER(:searchKeywordLike)
                    OR LOWER(v.sku) LIKE LOWER(:searchKeywordLike)
                )
                """);
        }

        if (categoryId != null) {
            conditions.add("p.category_id = :categoryId");
        }
        if (supplierId != null) {
            conditions.add("p.supplier_id = :supplierId");
        }

        boolean hasVariantFilters = useFullTextSearch; // FTS forces variant join

        if (minPrice != null) {
            conditions.add("v.selling_price >= :minPrice");
            hasVariantFilters = true;
        }
        if (maxPrice != null) {
            conditions.add("v.selling_price <= :maxPrice");
            hasVariantFilters = true;
        }

        // Dynamic spec filters
        if (specFilters != null && !specFilters.isEmpty()) {
            int specIndex = 0;
            for (Map.Entry<String, List<String>> entry : specFilters.entrySet()) {
                String specKey = entry.getKey();
                List<String> values = entry.getValue();

                if (values.isEmpty()) continue;

                if (values.size() == 1) {
                    conditions.add(String.format(
                            "(v.variant_specs::jsonb->>'%s' = :spec_%d_val_0 OR v.variant_specs LIKE :spec_%d_like_0)",
                            specKey, specIndex, specIndex
                    ));
                } else {
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

        // Build JOIN clause
        String joinClause = """
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            """;

        if (hasVariantFilters) {
            joinClause += "LEFT JOIN product_variants v ON v.product_id = p.id AND v.is_deleted = false";
        }

        // SELECT clause với ranking (nếu có FTS)
        String selectClause = useFullTextSearch
                ? "SELECT DISTINCT p.*, ts_rank(p.search_vector, plainto_tsquery('simple', :searchKeyword)) as rank"
                : "SELECT DISTINCT p.*";

        // Build main query
        String whereClause = String.join(" AND ", conditions);

        String sql = String.format("""
            %s
            FROM products p
            %s
            WHERE %s
            """, selectClause, joinClause, whereClause);

        // Count query (không cần ranking)
        String countSql = String.format("""
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            %s
            WHERE %s
            """, joinClause, whereClause);

        // Add ORDER BY
        String orderBy = buildOrderByClause(pageable.getSort(), useFullTextSearch);
        sql += orderBy;

        log.debug("Generated FTS SQL: {}", sql);
        log.debug("Search: '{}', Filters: {}", searchKeyword, specFilters);

        Query query = entityManager.createNativeQuery(sql, Product.class);
        Query countQuery = entityManager.createNativeQuery(countSql);

        setParameters(query, categoryId, supplierId, minPrice, maxPrice, searchKeyword, specFilters);
        setParameters(countQuery, categoryId, supplierId, minPrice, maxPrice, searchKeyword, specFilters);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Product> products = query.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        log.info("FTS returned {} products out of {} total", products.size(), total);

        return new PageImpl<>(products, pageable, total);
    }

    /**
     * Build ORDER BY clause
     * FTS: Order by rank (relevance) first, then by other fields
     */
    private String buildOrderByClause(Sort sort, boolean useFullTextSearch) {
        List<String> orderClauses = new ArrayList<>();

        // If FTS, add rank as primary sort
        if (useFullTextSearch) {
            orderClauses.add("rank DESC");
        }

        // Add user-specified sorts
        if (sort != null && sort.isSorted()) {
            for (Sort.Order order : sort) {
                String property = mapPropertyToColumn(order.getProperty());
                String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
                orderClauses.add(property + " " + direction);
            }
        } else if (!useFullTextSearch) {
            // Default sort if no FTS and no user sort
            orderClauses.add("p.created_at DESC");
        }

        return orderClauses.isEmpty() ? "" : " ORDER BY " + String.join(", ", orderClauses);
    }

    /**
     * Map entity property names to database column names
     */
    private String mapPropertyToColumn(String property) {
        return switch (property.toLowerCase()) {
            case "name" -> "p.name";
            case "createdat" -> "p.created_at";
            case "updatedat" -> "p.updated_at";
            case "price" -> "v.selling_price";
            default -> "p." + property;
        };
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
            String searchKeyword,
            Map<String, List<String>> specFilters) {

        // Full-text search parameters
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            query.setParameter("searchKeyword", searchKeyword);
            query.setParameter("searchKeywordLike", "%" + searchKeyword + "%");
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

        // Spec filter parameters
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