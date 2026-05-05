package com.locnguyen.ecommerce.domains.product.service;

import com.locnguyen.ecommerce.common.utils.SearchTextNormalizer;
import com.locnguyen.ecommerce.domains.brand.entity.Brand;
import com.locnguyen.ecommerce.domains.category.entity.Category;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the denormalized {@code products.search_text} column.
 *
 * <p>Combines product name, slug, brand name and category names through
 * {@link SearchTextNormalizer} so that FULLTEXT queries can match on either
 * raw text or accent-stripped text.
 *
 * <p>Extension points (intentionally not yet wired): SKU, attribute values,
 * color, size, material can be appended here when the catalog model evolves.
 */
@Component
public class ProductSearchTextBuilder {

    /**
     * Build the search_text payload for a product.
     * Always returns a non-null string ("" if every input is blank).
     */
    public String build(Product product) {
        if (product == null) return "";

        List<String> parts = new ArrayList<>();
        parts.add(product.getName());
        parts.add(product.getSlug());

        Brand brand = product.getBrand();
        if (brand != null) {
            parts.add(brand.getName());
        }

        Set<Category> categories = product.getCategories();
        if (categories != null) {
            for (Category category : categories) {
                if (category != null) {
                    parts.add(category.getName());
                }
            }
        }

        return SearchTextNormalizer.normalizeAndJoin(parts.toArray(new String[0]));
    }
}
