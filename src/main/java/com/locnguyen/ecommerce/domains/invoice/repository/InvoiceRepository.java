package com.locnguyen.ecommerce.domains.invoice.repository;

import com.locnguyen.ecommerce.domains.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

import java.util.UUID;
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>,
        JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByOrderId(UUID orderId);

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    boolean existsByOrderId(UUID orderId);
}
