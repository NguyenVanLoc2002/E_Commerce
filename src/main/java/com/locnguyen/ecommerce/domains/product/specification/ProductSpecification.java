package com.locnguyen.ecommerce.domains.product.specification;

import com.locnguyen.ecommerce.domains.product.dto.ProductFilter;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import jakarta.persistence.criteria.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specification for product list queries.
 * Builds predicates from optional {@link ProductFilter} fields.
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> withFilter(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword — search product name
            if (StringUtils.hasText(filter.getKeyword())) {
                String pattern = "%" + filter.getKeyword().toLowerCase().trim() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            // Category — join product_categories
            if (filter.getCategoryId() != null) {
                Join<Object, Object> categoryJoin = root.join("categories", JoinType.INNER);
                predicates.add(cb.equal(categoryJoin.get("id"), filter.getCategoryId()));
            }

            // Brand
            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
            }

            // Status
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Featured
            if (filter.getFeatured() != null) {
                predicates.add(cb.equal(root.get("featured"), filter.getFeatured()));
            }

            // Price range — subquery against variant prices
            if (filter.getMinPrice() != null) {
                predicates.add(cb.exists(variantPriceSubquery(root, cb,
                        cb.ge(filter.getMinPrice()))));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.exists(variantPriceSubquery(root, cb,
                        cb.le(filter.getMaxPrice()))));
            }

            return predicates.isEmpty()
                    ? null
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Builds a subquery: "EXISTS (SELECT 1 FROM product_variants WHERE product_id = :id AND price_op)"
     * Uses COALESCE(sale_price, base_price) — sale_price if set, otherwise base_price.
     */
    private static Subquery<Long> variantPriceSubquery(
            Root<Product> root, CriteriaBuilder cb, Predicate priceOp) {

        Subquery<Long> sq = query.subquery(Long.class);
        Root<ProductVariant> pv = sq.from(ProductVariant.class);

        Expression<BigDecimal> effectivePrice = cb.coalesce(
                pv.get("salePrice"), pv.get("basePrice"));

        sq.select(cb.literal(1L));
        sq.where(
                cb.equal(pv.get("product"), root),
                priceOp
        );
        return sq;
    }
}
