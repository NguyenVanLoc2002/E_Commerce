package com.locnguyen.ecommerce.domains.payment.repository;

import com.locnguyen.ecommerce.domains.payment.entity.PaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentConfigRepository extends JpaRepository<PaymentConfig, UUID> {

    Optional<PaymentConfig> findByProviderIgnoreCase(String provider);
}
