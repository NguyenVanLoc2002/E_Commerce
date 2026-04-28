package com.locnguyen.ecommerce.domains.customer.repository;

import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.UUID;
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUserId(UUID userId);
}
