package com.locnguyen.ecommerce.domains.product.specification;

import com.locnguyen.ecommerce.domains.product.dto.ProductFilter;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import com.locnguyen.ecommerce.domains.product.enums.ProductStatus;
import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductSpecificationTest {

    @Mock Root<Product> root;
    @Mock CriteriaQuery<Object> query;
    @Mock CriteriaBuilder cb;

    @Mock Path<Boolean> deletedPath;
    @Mock Path<String> namePath;
    @Mock Path<ProductStatus> statusPath;
    @Mock Path<Boolean> featuredPath;
    @Mock Predicate notDeleted;
    @Mock Predicate combined;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void keyword_filter_no_longer_produces_a_like_predicate() {
        ProductFilter filter = ProductFilter.builder().keyword("anything").build();

        when(root.get("deleted")).thenReturn((Path) deletedPath);
        when(cb.isFalse(deletedPath)).thenReturn(notDeleted);
        when(cb.and(any(Predicate[].class))).thenReturn(combined);

        ProductSpecification.withFilter(filter).toPredicate(root, query, cb);

        // The old LIKE-based keyword predicate is gone — keyword routing now lives
        // in ProductSearchRepository (FULLTEXT). The Specification must not call cb.like
        // for the product name search anymore.
        verify(cb, never()).like(any(Expression.class), any(String.class));
        verify(cb, never()).lower(any(Expression.class));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void price_range_uses_a_single_subquery_so_min_and_max_match_the_same_variant() {
        ProductFilter filter = ProductFilter.builder()
                .minPrice(new BigDecimal("100"))
                .maxPrice(new BigDecimal("500"))
                .build();

        Subquery<Long> sq = (Subquery<Long>) org.mockito.Mockito.mock(Subquery.class);
        Root<ProductVariant> pvRoot = (Root<ProductVariant>) org.mockito.Mockito.mock(Root.class);
        Path<Object> productPath = (Path<Object>) org.mockito.Mockito.mock(Path.class);
        Path<BigDecimal> salePath = (Path<BigDecimal>) org.mockito.Mockito.mock(Path.class);
        Path<BigDecimal> basePath = (Path<BigDecimal>) org.mockito.Mockito.mock(Path.class);
        Path<Boolean> variantDeletedPath = (Path<Boolean>) org.mockito.Mockito.mock(Path.class);
        Expression<BigDecimal> coalesced = (Expression<BigDecimal>) org.mockito.Mockito.mock(Expression.class);
        Expression<Long> literalOne = (Expression<Long>) org.mockito.Mockito.mock(Expression.class);
        Predicate variantDeletedFalse = org.mockito.Mockito.mock(Predicate.class);
        Predicate productEq = org.mockito.Mockito.mock(Predicate.class);
        Predicate gePred = org.mockito.Mockito.mock(Predicate.class);
        Predicate lePred = org.mockito.Mockito.mock(Predicate.class);
        Predicate exists = org.mockito.Mockito.mock(Predicate.class);

        when(root.get("deleted")).thenReturn((Path) deletedPath);
        when(cb.isFalse(deletedPath)).thenReturn(notDeleted);
        when(query.subquery(Long.class)).thenReturn(sq);
        when(sq.from(ProductVariant.class)).thenReturn(pvRoot);
        when(pvRoot.get("salePrice")).thenReturn((Path) salePath);
        when(pvRoot.get("basePrice")).thenReturn((Path) basePath);
        when(pvRoot.get("product")).thenReturn(productPath);
        when(pvRoot.get("deleted")).thenReturn((Path) variantDeletedPath);
        when(cb.isFalse(variantDeletedPath)).thenReturn(variantDeletedFalse);
        when(cb.coalesce(salePath, basePath)).thenReturn(coalesced);
        when(cb.equal(productPath, root)).thenReturn(productEq);
        when(cb.ge(coalesced, filter.getMinPrice())).thenReturn(gePred);
        when(cb.le(coalesced, filter.getMaxPrice())).thenReturn(lePred);
        when(cb.literal(1L)).thenReturn(literalOne);
        when(cb.exists(sq)).thenReturn(exists);
        when(sq.select(any())).thenReturn(sq);
        when(cb.and(any(Predicate[].class))).thenReturn(combined);

        ProductSpecification.withFilter(filter).toPredicate(root, query, cb);

        verify(query, atLeastOnce()).subquery(Long.class);
        // Exactly one subquery — same variant for min and max.
        verify(query).subquery(Long.class);
        verify(cb).isFalse(variantDeletedPath); // soft-delete on variant honored
        verify(cb).ge(coalesced, filter.getMinPrice());
        verify(cb).le(coalesced, filter.getMaxPrice());
    }
}
