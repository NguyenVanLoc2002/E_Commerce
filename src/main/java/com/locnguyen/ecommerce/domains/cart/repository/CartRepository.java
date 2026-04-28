package com.locnguyen.ecommerce.domains.cart.repository;

import com.locnguyen.ecommerce.domains.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.UUID;
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, com.locnguyen.ecommerce.domains.cart.enums.CartStatus status);
}
