package com.locnguyen.ecommerce.domains.productvariant.repository;

import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(Long productId);

    long countByProductId(Long productId);
}
