package com.locnguyen.ecommerce.domains.product.repository;

import com.locnguyen.ecommerce.domains.product.dto.ProductFilter;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Native FULLTEXT implementation of {@link ProductSearchRepository}.
 *
 * <p>The query body is built once and shared between the count and select
 * statements so filter semantics stay identical between the two.
 */
@RequiredArgsConstructor
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS = new LinkedHashSet<>(List.of(
            "created_at", "updated_at", "name", "status", "is_featured"
    ));

    private static final Map<String, String> SORT_PROPERTY_TO_COLUMN = Map.of(
            "createdAt", "created_at",
            "updatedAt", "updated_at",
            "name", "name",
            "status", "status",
            "featured", "is_featured"
    );

    private final EntityManager entityManager;

    @Override
    public Page<Product> searchByKeyword(String normalizedKeyword,
                                         ProductFilter filter,
                                         Pageable pageable) {

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", normalizedKeyword);

        StringBuilder where = new StringBuilder("""
                WHERE MATCH(p.name, p.slug, p.search_text) AGAINST (:keyword IN BOOLEAN MODE)
                """);

        appendSoftDeleteClause(where, filter);
        appendEqualClause(where, params, filter, "status", "p.status", true);
        appendEqualClause(where, params, filter, "brandId", "p.brand_id", false);
        appendEqualClause(where, params, filter, "featured", "p.is_featured", false);
        appendCategoryClause(where, params, filter);
        appendPriceRangeClause(where, params, filter);

        String selectSql =
                "SELECT p.id FROM products p " + where +
                        " ORDER BY MATCH(p.name, p.slug, p.search_text) " +
                        "AGAINST (:keyword IN BOOLEAN MODE) DESC" +
                        appendPageableSort(pageable);

        String countSql = "SELECT COUNT(*) FROM products p " + where;

        Query countQuery = entityManager.createNativeQuery(countSql);
        bind(countQuery, params);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Query idQuery = entityManager.createNativeQuery(selectSql);
        bind(idQuery, params);
        idQuery.setFirstResult((int) pageable.getOffset());
        idQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object> rawIds = idQuery.getResultList();
        if (rawIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<UUID> ids = rawIds.stream()
                .map(ProductSearchRepositoryImpl::toUuid)
                .toList();

        // Load entities in a second step (preserve search ranking order).
        List<Product> entities = entityManager.createQuery(
                        "SELECT DISTINCT p FROM Product p " +
                                "LEFT JOIN FETCH p.brand " +
                                "LEFT JOIN FETCH p.categories " +
                                "WHERE p.id IN :ids",
                        Product.class)
                .setParameter("ids", ids)
                .getResultList();

        Map<UUID, Product> byId = new HashMap<>();
        for (Product p : entities) byId.put(p.getId(), p);
        List<Product> ordered = ids.stream()
                .map(byId::get)
                .filter(p -> p != null)
                .toList();

        return new PageImpl<>(ordered, pageable, total);
    }

    // ─── WHERE builders ───────────────────────────────────────────────────────

    private void appendSoftDeleteClause(StringBuilder where, ProductFilter filter) {
        Boolean includeDeleted = filter != null ? filter.getIncludeDeleted() : null;
        Boolean isDeleted = filter != null ? filter.getIsDeleted() : null;

        if (Boolean.TRUE.equals(includeDeleted)) {
            return;
        }
        if (isDeleted != null) {
            where.append(" AND p.is_deleted = ").append(isDeleted ? "TRUE" : "FALSE");
            return;
        }
        where.append(" AND p.is_deleted = FALSE");
    }

    private void appendEqualClause(StringBuilder where,
                                   Map<String, Object> params,
                                   ProductFilter filter,
                                   String filterField,
                                   String column,
                                   boolean isString) {
        if (filter == null) return;
        Object value = readFilter(filter, filterField);
        if (value == null) return;

        where.append(" AND ").append(column).append(" = :").append(filterField);
        params.put(filterField, isString ? value.toString() : value);
    }

    private void appendCategoryClause(StringBuilder where,
                                      Map<String, Object> params,
                                      ProductFilter filter) {
        if (filter == null || filter.getCategoryId() == null) return;
        where.append(" AND EXISTS (SELECT 1 FROM product_categories pc " +
                "WHERE pc.product_id = p.id AND pc.category_id = :categoryId)");
        params.put("categoryId", filter.getCategoryId().toString());
    }

    /**
     * Price range must reference the SAME variant so a product whose cheap
     * variant matches min and whose expensive variant matches max is not
     * incorrectly returned for narrow [min, max] windows.
     */
    private void appendPriceRangeClause(StringBuilder where,
                                        Map<String, Object> params,
                                        ProductFilter filter) {
        if (filter == null) return;
        BigDecimal min = filter.getMinPrice();
        BigDecimal max = filter.getMaxPrice();
        if (min == null && max == null) return;

        StringBuilder pv = new StringBuilder(
                " AND EXISTS (SELECT 1 FROM product_variants pv " +
                "WHERE pv.product_id = p.id AND pv.is_deleted = FALSE");
        if (min != null) {
            pv.append(" AND COALESCE(pv.sale_price, pv.base_price) >= :minPrice");
            params.put("minPrice", min);
        }
        if (max != null) {
            pv.append(" AND COALESCE(pv.sale_price, pv.base_price) <= :maxPrice");
            params.put("maxPrice", max);
        }
        pv.append(")");
        where.append(pv);
    }

    private String appendPageableSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) return "";

        StringBuilder sb = new StringBuilder();
        for (Sort.Order order : sort) {
            String column = SORT_PROPERTY_TO_COLUMN.get(order.getProperty());
            if (column == null || !ALLOWED_SORT_COLUMNS.contains(column)) {
                continue; // silently skip unknown sort fields — keeps API stable
            }
            sb.append(", p.").append(column).append(' ')
                    .append(order.isAscending() ? "ASC" : "DESC");
        }
        return sb.toString();
    }

    private static Object readFilter(ProductFilter filter, String name) {
        return switch (name) {
            case "status" -> filter.getStatus();
            case "brandId" -> filter.getBrandId() == null ? null : filter.getBrandId().toString();
            case "featured" -> filter.getFeatured();
            default -> null;
        };
    }

    private static void bind(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private static UUID toUuid(Object raw) {
        if (raw instanceof UUID u) return u;
        if (raw instanceof byte[] bytes) {
            // MariaDB CHAR(36) returns String; binary path kept for safety.
            return UUID.fromString(new String(bytes));
        }
        return UUID.fromString(raw.toString());
    }
}
