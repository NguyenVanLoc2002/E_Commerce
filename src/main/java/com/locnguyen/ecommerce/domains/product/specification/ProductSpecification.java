package com.locnguyen.ecommerce.domains.product.specification;

import com.locnguyen.ecommerce.common.specification.SoftDeleteSpecificationHelper;
import com.locnguyen.ecommerce.domains.product.dto.ProductFilter;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specification for product list queries.
 *
 * <p>Handles non-keyword filters only. Keyword search is routed to the
 * FULLTEXT path in
 * {@link com.locnguyen.ecommerce.domains.product.repository.ProductSearchRepository}
 * — see {@link com.locnguyen.ecommerce.domains.product.service.impl.ProductServiceImpl}.
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> withFilter(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            SoftDeleteSpecificationHelper.addDeletedFilter(
                    predicates,
                    root.get("deleted"),
                    cb,
                    filter != null ? filter.getIsDeleted() : null,
                    filter != null ? filter.getIncludeDeleted() : null
            );

            if (filter != null && filter.getCategoryId() != null) {
                Join<Object, Object> categoryJoin = root.join("categories", JoinType.INNER);
                predicates.add(cb.equal(categoryJoin.get("id"), filter.getCategoryId()));
            }

            if (filter != null && filter.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), filter.getBrandId()));
            }

            if (filter != null && filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter != null && filter.getFeatured() != null) {
                predicates.add(cb.equal(root.get("featured"), filter.getFeatured()));
            }

            // Price range: min and max must match the SAME variant, otherwise a product
            // whose cheap variant satisfies min and whose expensive variant satisfies
            // max would be incorrectly returned for narrow [min, max] windows.
            // Variant soft-delete is honored.
            if (filter != null && (filter.getMinPrice() != null || filter.getMaxPrice() != null)) {
                Subquery<Long> sq = query.subquery(Long.class);
                Root<ProductVariant> pv = sq.from(ProductVariant.class);
                Expression<BigDecimal> price = cb.coalesce(pv.get("salePrice"), pv.get("basePrice"));

                List<Predicate> variantPredicates = new ArrayList<>();
                variantPredicates.add(cb.equal(pv.get("product"), root));
                variantPredicates.add(cb.isFalse(pv.get("deleted")));
                if (filter.getMinPrice() != null) {
                    variantPredicates.add(cb.ge(price, filter.getMinPrice()));
                }
                if (filter.getMaxPrice() != null) {
                    variantPredicates.add(cb.le(price, filter.getMaxPrice()));
                }
                sq.select(cb.literal(1L))
                        .where(variantPredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(sq));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
