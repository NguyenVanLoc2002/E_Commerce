package com.locnguyen.ecommerce.domains.product.service;

import com.locnguyen.ecommerce.domains.product.entity.Product;
import com.locnguyen.ecommerce.domains.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds {@code products.search_text} for existing products in batches.
 *
 * <p>Each batch runs in its own transaction so a failure mid-way does not
 * roll the whole reindex back. Only active rows are reindexed because the
 * {@code @SQLRestriction} on {@code SoftDeleteEntity} hides deleted ones
 * from JPA queries — they keep the value written at delete time, which is
 * acceptable since admin keyword search of deleted products is rare.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchReindexService {

    private static final int DEFAULT_BATCH_SIZE = 200;

    private final ProductRepository productRepository;
    private final ProductSearchTextBuilder productSearchTextBuilder;

    /** Rebuild every product's search_text using {@link #DEFAULT_BATCH_SIZE}. */
    public ReindexResult reindexAll() {
        return reindexAll(DEFAULT_BATCH_SIZE);
    }

    public ReindexResult reindexAll(int batchSize) {
        long total = 0;
        int page = 0;
        Page<Product> slice;
        do {
            slice = reindexPage(page, batchSize);
            total += slice.getNumberOfElements();
            page++;
        } while (slice.hasNext());

        log.info("Product search_text reindex completed: {} rows", total);
        return new ReindexResult(total);
    }

    /**
     * Reindex a single page in its own transaction. Public so the page-by-page
     * progress can be monitored; the {@link Specification} matches both active
     * and soft-deleted rows.
     */
    @Transactional
    public Page<Product> reindexPage(int page, int batchSize) {
        Specification<Product> includeAll = (root, query, cb) -> cb.conjunction();
        Page<Product> slice = productRepository.findAll(
                includeAll,
                PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "createdAt")));

        for (Product product : slice.getContent()) {
            product.setSearchText(productSearchTextBuilder.build(product));
        }
        return slice;
    }

    public record ReindexResult(long totalProcessed) {}
}
