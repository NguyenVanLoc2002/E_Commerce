package com.locnguyen.ecommerce.domains.productvariant.repository;

import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.util.UUID;
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    List<ProductVariant> findByProductIdOrderByCreatedAtAsc(UUID productId);

    long countByProductId(UUID productId);
}
