package com.locnguyen.ecommerce.domains.payment.service;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.utils.CodeGenerator;
import com.locnguyen.ecommerce.domains.order.entity.Order;
import com.locnguyen.ecommerce.domains.order.enums.PaymentStatus;
import com.locnguyen.ecommerce.domains.order.repository.OrderRepository;
import com.locnguyen.ecommerce.domains.payment.dto.*;
import com.locnguyen.ecommerce.domains.payment.entity.Payment;
import com.locnguyen.ecommerce.domains.payment.entity.PaymentTransaction;
import com.locnguyen.ecommerce.domains.payment.enums.PaymentRecordStatus;
import com.locnguyen.ecommerce.domains.payment.enums.TransactionStatus;
import com.locnguyen.ecommerce.domains.payment.mapper.PaymentMapper;
import com.locnguyen.ecommerce.domains.payment.repository.PaymentRepository;
import com.locnguyen.ecommerce.domains.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;

    // ─── COD payment ────────────────────────────────────────────────────────

    /**
     * Create a COD payment record for an order.
     * Called by OrderService after order creation.
     */
    @Transactional
    public PaymentResponse createCodPayment(Order order) {
        if (paymentRepository.existsByOrderId(order.getId())) {
            log.warn("Payment already exists for order: code={}", order.getOrderCode());
            return getPaymentByOrderId(order.getId());
        }

        String paymentCode = CodeGenerator.generatePaymentTransactionCode();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentCode(paymentCode);
        payment.setMethod("COD");
        payment.setStatus(PaymentRecordStatus.PENDING);
        payment.setAmount(order.getTotalAmount());

        payment = paymentRepository.save(payment);

        recordTransaction(payment, TransactionStatus.INITIATED, "COD", null,
                "COD payment created for order " + order.getOrderCode());

        log.info("COD payment created: code={} orderCode={} amount={}",
                paymentCode, order.getOrderCode(), order.getTotalAmount());
        return paymentMapper.toResponse(payment);
    }

    /**
     * Mark COD payment as paid — called on delivery confirmation.
     */
    @Transactional
    public PaymentResponse completeCodPayment(Long orderId) {
        Payment payment = findByOrderIdOrThrow(orderId);

        if (payment.getStatus() != PaymentRecordStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "Payment is already in status " + payment.getStatus());
        }

        payment.setStatus(PaymentRecordStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        recordTransaction(payment, TransactionStatus.SUCCESS, "COD", null,
                "COD payment collected for order " + order.getOrderCode());

        log.info("COD payment completed: code={} orderCode={}",
                payment.getPaymentCode(), order.getOrderCode());
        return paymentMapper.toResponse(payment);
    }

    // ─── Online payment flow ────────────────────────────────────────────────

    /**
     * Initiate an online payment for an order.
     *
     * <p>Idempotency: if a payment already exists for the order, returns it
     * instead of creating a duplicate.
     */
    @Transactional
    public PaymentResponse initiatePayment(Order order, InitPaymentRequest request) {
        if (paymentRepository.existsByOrderId(order.getId())) {
            return getPaymentByOrderId(order.getId());
        }

        String paymentCode = CodeGenerator.generatePaymentTransactionCode();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentCode(paymentCode);
        payment.setMethod("ONLINE");
        payment.setStatus(PaymentRecordStatus.INITIATED);
        payment.setAmount(order.getTotalAmount());
        payment.setExpiredAt(LocalDateTime.now().plusHours(24));

        payment = paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        recordTransaction(payment, TransactionStatus.INITIATED, "ONLINE", request.getProvider(),
                "Online payment initiated for order " + order.getOrderCode());

        log.info("Online payment initiated: code={} orderCode={} provider={}",
                paymentCode, order.getOrderCode(), request.getProvider());
        return paymentMapper.toResponse(payment);
    }

    /**
     * Process a payment callback from a payment gateway.
     *
     * <p>Idempotency at multiple levels:
     * <ul>
     *   <li>Already PAID → return existing record</li>
     *   <li>Already REFUNDED → reject</li>
     *   <li>Duplicate providerTxnId → return existing record</li>
     * </ul>
     *
     * <p>On SUCCESS: payment → PAID, order.paymentStatus → PAID.
     * <p>On FAILED: payment → FAILED, order.paymentStatus → FAILED.
     */
    @Transactional
    public PaymentResponse processCallback(PaymentCallbackRequest request) {
        Order order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_CALLBACK_INVALID,
                        "Order not found for code " + request.getOrderCode()));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // Already paid — idempotent no-op
        if (payment.getStatus() == PaymentRecordStatus.PAID) {
            log.warn("Payment callback ignored (already paid): code={}", payment.getPaymentCode());
            return paymentMapper.toResponse(payment);
        }
        if (payment.getStatus() == PaymentRecordStatus.REFUNDED
                || payment.getStatus() == PaymentRecordStatus.PARTIALLY_REFUNDED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "Payment has been refunded and cannot be changed");
        }

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(request.getStatus());
        TransactionStatus txnStatus = isSuccess ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;

        // Duplicate provider transaction ID — idempotent no-op
        if (request.getProviderTxnId() != null
                && transactionRepository.findByProviderTxnId(request.getProviderTxnId()).isPresent()) {
            log.warn("Duplicate provider transaction ID: {}", request.getProviderTxnId());
            return paymentMapper.toResponse(payment);
        }

        if (isSuccess) {
            payment.setStatus(PaymentRecordStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentRecordStatus.FAILED);
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        payment = paymentRepository.save(payment);
        orderRepository.save(order);

        PaymentTransaction txn = recordTransaction(payment, txnStatus, "ONLINE",
                request.getProviderTxnId(),
                isSuccess ? "Payment successful" : "Payment failed");
        txn.setReferenceType("CALLBACK");
        txn.setReferenceId(request.getOrderCode());
        txn.setPayload(request.getPayload());
        transactionRepository.save(txn);

        log.info("Payment callback processed: code={} orderCode={} success={}",
                payment.getPaymentCode(), order.getOrderCode(), isSuccess);
        return paymentMapper.toResponse(payment);
    }

    // ─── Read operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentMapper.toResponse(findByOrderIdOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByCode(String paymentCode) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long paymentId) {
        List<PaymentTransaction> transactions = transactionRepository
                .findByPaymentIdOrderByCreatedAtDesc(paymentId);
        return paymentMapper.toTransactionResponses(transactions);
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private Payment findByOrderIdOrThrow(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentTransaction recordTransaction(Payment payment, TransactionStatus status,
                                                 String method, String providerTxnId,
                                                 String note) {
        PaymentTransaction txn = new PaymentTransaction();
        txn.setPayment(payment);
        txn.setTransactionCode(CodeGenerator.generatePaymentTransactionCode());
        txn.setStatus(status);
        txn.setAmount(payment.getAmount());
        txn.setMethod(method);
        txn.setProvider(providerTxnId != null ? "ONLINE" : null);
        txn.setProviderTxnId(providerTxnId);
        txn.setNote(note);
        return transactionRepository.save(txn);
    }
}
