package com.locnguyen.ecommerce.domains.review.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product review")
public class ReviewResponse {

    private final Long id;

    // ─── Product info ────────────────────────────────────────────────────────
    private final Long productId;
    private final String productName;

    // ─── Order link ──────────────────────────────────────────────────────────
    private final Long orderItemId;
    private final Long orderId;
    private final String orderCode;

    // ─── Customer info (visible in admin views; hidden in public listing) ────
    private final Long customerId;
    private final String customerName;

    // ─── Review content ──────────────────────────────────────────────────────
    private final int rating;
    private final String title;
    private final String body;
    private final String status;

    /** Internal note — only populated for admin responses. */
    private final String adminNote;

    private final LocalDateTime createdAt;
}
