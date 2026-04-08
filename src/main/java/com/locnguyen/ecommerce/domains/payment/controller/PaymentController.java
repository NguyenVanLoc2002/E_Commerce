package com.locnguyen.ecommerce.domains.payment.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.payment.dto.InitPaymentRequest;
import com.locnguyen.ecommerce.domains.payment.dto.PaymentCallbackRequest;
import com.locnguyen.ecommerce.domains.payment.dto.PaymentResponse;
import com.locnguyen.ecommerce.domains.payment.dto.TransactionResponse;
import com.locnguyen.ecommerce.domains.payment.service.PaymentService;
import com.locnguyen.ecommerce.domains.order.entity.Order;
import com.locnguyen.ecommerce.domains.order.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment", description = "Payment management")
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_V1 + "/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    // ─── Customer endpoints ─────────────────────────────────────────────────

    @Operation(summary = "Get payment by order ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(paymentService.getPaymentByOrderId(orderId));
    }

    @Operation(summary = "Initiate online payment for an order")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/order/{orderId}/initiate")
    public ApiResponse<PaymentResponse> initiatePayment(
            @PathVariable Long orderId,
            @RequestBody(required = false) InitPaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.locnguyen.ecommerce.common.exception.AppException(
                        com.locnguyen.ecommerce.common.exception.ErrorCode.ORDER_NOT_FOUND));
        if (request == null) {
            request = new InitPaymentRequest();
        }
        return ApiResponse.created(paymentService.initiatePayment(order, request));
    }

    @Operation(summary = "Get payment transaction history")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{paymentId}/transactions")
    public ApiResponse<List<TransactionResponse>> getTransactions(@PathVariable Long paymentId) {
        return ApiResponse.success(paymentService.getTransactions(paymentId));
    }

    // ─── Gateway callback (no auth required) ────────────────────────────────

    @Operation(summary = "[Gateway] Payment callback endpoint")
    @PostMapping("/callback")
    public ApiResponse<PaymentResponse> processCallback(
            @Valid @RequestBody PaymentCallbackRequest request) {
        return ApiResponse.success(paymentService.processCallback(request));
    }
}
