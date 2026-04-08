package com.locnguyen.ecommerce.domains.payment.mapper;

import com.locnguyen.ecommerce.domains.payment.dto.PaymentResponse;
import com.locnguyen.ecommerce.domains.payment.dto.TransactionResponse;
import com.locnguyen.ecommerce.domains.payment.entity.Payment;
import com.locnguyen.ecommerce.domains.payment.entity.PaymentTransaction;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    default PaymentResponse toResponse(Payment payment) {
        if (payment == null) return null;

        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderCode(payment.getOrder().getOrderCode())
                .paymentCode(payment.getPaymentCode())
                .method(payment.getMethod())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt());

        if (payment.getTransactions() != null) {
            builder.transactions(toTransactionResponses(payment.getTransactions()));
        }

        return builder.build();
    }

    TransactionResponse toTransactionResponse(PaymentTransaction transaction);

    List<TransactionResponse> toTransactionResponses(List<PaymentTransaction> transactions);
}
