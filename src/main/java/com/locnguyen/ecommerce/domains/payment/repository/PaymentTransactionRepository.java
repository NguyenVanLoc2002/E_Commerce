package com.locnguyen.ecommerce.domains.payment.repository;

import com.locnguyen.ecommerce.domains.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import java.util.UUID;
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    Optional<PaymentTransaction> findByProviderTxnId(String providerTxnId);
}
