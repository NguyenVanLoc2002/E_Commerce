package com.locnguyen.ecommerce.domains.review.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.review.dto.CreateReviewRequest;
import com.locnguyen.ecommerce.domains.review.dto.ModerateReviewRequest;
import com.locnguyen.ecommerce.domains.review.dto.ReviewFilter;
import com.locnguyen.ecommerce.domains.review.dto.ReviewResponse;
import com.locnguyen.ecommerce.domains.review.dto.UpdateReviewStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(Customer customer, CreateReviewRequest request);

    ReviewResponse getReviewById(UUID id);

    PagedResponse<ReviewResponse> getProductReviews(UUID productId, ReviewFilter filter, Pageable pageable);

    PagedResponse<ReviewResponse> getMyReviews(Customer customer, Pageable pageable);

    PagedResponse<ReviewResponse> getPendingReviews(ReviewFilter filter, Pageable pageable);

    PagedResponse<ReviewResponse> listReviews(ReviewFilter filter, Pageable pageable);

    ReviewResponse adminGetById(UUID id);

    ReviewResponse moderateReview(UUID reviewId, UpdateReviewStatusRequest request);

    ReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request);

    void deleteReview(UUID reviewId);
}
