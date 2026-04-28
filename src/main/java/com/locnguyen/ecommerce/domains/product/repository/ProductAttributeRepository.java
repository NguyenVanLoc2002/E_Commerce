package com.locnguyen.ecommerce.domains.product.repository;

import com.locnguyen.ecommerce.domains.product.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.UUID;
@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {

    boolean existsByCode(String code);

    Optional<ProductAttribute> findByCode(String code);
}
