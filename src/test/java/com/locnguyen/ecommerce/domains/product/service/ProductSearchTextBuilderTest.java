package com.locnguyen.ecommerce.domains.product.service;

import com.locnguyen.ecommerce.domains.brand.entity.Brand;
import com.locnguyen.ecommerce.domains.category.entity.Category;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchTextBuilderTest {

    private final ProductSearchTextBuilder builder = new ProductSearchTextBuilder();

    @Test
    void combines_name_slug_brand_and_categories_into_normalized_string() {
        Brand brand = new Brand();
        brand.setName("Local Brand Việt");

        Category top = new Category();
        top.setName("Áo");
        Category sub = new Category();
        sub.setName("Áo Thun");

        Product product = new Product();
        product.setName("Áo Thun Basic Nam");
        product.setSlug("ao-thun-basic-nam");
        product.setBrand(brand);
        Set<Category> categories = new LinkedHashSet<>();
        categories.add(top);
        categories.add(sub);
        product.setCategories(categories);

        String result = builder.build(product);

        assertThat(result).contains("ao thun basic nam");
        assertThat(result).contains("ao-thun-basic-nam");
        assertThat(result).contains("local brand viet");
        assertThat(result).contains("ao thun");
    }

    @Test
    void handles_null_brand_and_empty_categories() {
        Product product = new Product();
        product.setName("Quần Jean");
        product.setSlug("quan-jean");

        String result = builder.build(product);

        assertThat(result).isEqualTo("quan jean quan-jean");
    }

    @Test
    void null_product_returns_empty_string() {
        assertThat(builder.build(null)).isEmpty();
    }
}
