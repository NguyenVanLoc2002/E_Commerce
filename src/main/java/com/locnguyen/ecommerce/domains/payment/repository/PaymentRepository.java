package com.locnguyen.ecommerce.domains.payment.repository;

import com.locnguyen.ecommerce.domains.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

import java.util.UUID;
public interface PaymentRepository extends JpaRepository<Payment, UUID>,
        JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByPaymentCode(String paymentCode);

    boolean existsByOrderId(UUID orderId);
}
