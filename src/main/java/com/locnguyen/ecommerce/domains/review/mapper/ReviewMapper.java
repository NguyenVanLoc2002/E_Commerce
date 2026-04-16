package com.locnguyen.ecommerce.domains.review.mapper;

import com.locnguyen.ecommerce.domains.review.dto.ReviewResponse;
import com.locnguyen.ecommerce.domains.review.entity.Review;
import com.locnguyen.ecommerce.domains.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    /**
     * Public response — omits internal admin note and customer identity.
     * Used for product listing pages.
     */
    default ReviewResponse toPublicResponse(Review review) {
        if (review == null) return null;

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .status(review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Customer's own review response — includes order reference for context.
     */
    default ReviewResponse toOwnerResponse(Review review) {
        if (review == null) return null;

        var order = review.getOrderItem().getOrder();

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .orderItemId(review.getOrderItem().getId())
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .status(review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Full admin response — includes customer identity and internal admin note.
     */
    default ReviewResponse toAdminResponse(Review review) {
        if (review == null) return null;

        var order = review.getOrderItem().getOrder();
        User user = review.getCustomer().getUser();
        String customerName = buildName(user.getFirstName(), user.getLastName());

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .orderItemId(review.getOrderItem().getId())
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .customerId(review.getCustomer().getId())
                .customerName(customerName)
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .status(review.getStatus().name())
                .adminNote(review.getAdminNote())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String buildName(String first, String last) {
        if (first == null && last == null) return "";
        if (first == null) return last;
        if (last == null) return first;
        return first + " " + last;
    }
}
