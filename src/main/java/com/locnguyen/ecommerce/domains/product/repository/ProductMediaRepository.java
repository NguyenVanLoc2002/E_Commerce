package com.locnguyen.ecommerce.domains.product.repository;

import com.locnguyen.ecommerce.domains.product.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdOrderBySortOrderAsc(Long productId);

    List<ProductMedia> findByProductIdAndVariantIdOrderBySortOrderAsc(Long productId, Long variantId);
}
