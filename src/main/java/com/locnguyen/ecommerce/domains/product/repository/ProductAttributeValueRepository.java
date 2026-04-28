package com.locnguyen.ecommerce.domains.product.repository;

import com.locnguyen.ecommerce.domains.product.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.UUID;
@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {

    Optional<ProductAttributeValue> findByAttributeCodeAndValue(String attributeCode, String value);
}
