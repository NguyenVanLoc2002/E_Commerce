package com.locnguyen.ecommerce.domains.product.repository;

import com.locnguyen.ecommerce.domains.product.dto.ProductFilter;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom repository fragment for FULLTEXT-based product keyword search.
 *
 * <p>Implementation uses a native MariaDB query with
 * {@code MATCH(name, slug, search_text) AGAINST (? IN BOOLEAN MODE)}
 * combined with the standard {@link ProductFilter} predicates.
 *
 * <p>Results are ordered by FULLTEXT relevance first; the {@link Pageable}'s
 * sort (if any) is appended after relevance to preserve existing list APIs.
 */
public interface ProductSearchRepository {

    /**
     * Run a keyword search.
     *
     * @param normalizedKeyword keyword already passed through
     *        {@link com.locnguyen.ecommerce.common.utils.SearchTextNormalizer}.
     *        Must not be blank — caller is expected to fall back to the
     *        Specification path when there is no keyword.
     * @param filter optional filters; null is treated as "no extra filter" but
     *        soft-delete defaults still apply (active-only).
     * @param pageable pagination + optional secondary sort.
     */
    Page<Product> searchByKeyword(String normalizedKeyword, ProductFilter filter, Pageable pageable);
}
