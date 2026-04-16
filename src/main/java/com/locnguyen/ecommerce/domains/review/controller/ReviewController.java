package com.locnguyen.ecommerce.domains.review.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.review.dto.CreateReviewRequest;
import com.locnguyen.ecommerce.domains.review.dto.ReviewResponse;
import com.locnguyen.ecommerce.domains.review.service.ReviewService;
import com.locnguyen.ecommerce.domains.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reviews", description = "Product reviews — submission (customers) and public listing")
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_V1 + "/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    @Operation(
            summary = "Submit a product review",
            description = "Only allowed after the order containing the product is COMPLETED. " +
                    "One review per customer per product. New reviews are PENDING until moderated."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.created(
                reviewService.createReview(request, userService.getCurrentCustomer()));
    }

    @Operation(summary = "Get my submitted reviews")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ApiResponse<PagedResponse<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(
                reviewService.getMyReviews(userService.getCurrentCustomer(), pageable));
    }

    @Operation(
            summary = "Get approved reviews for a product",
            description = "Public endpoint — returns only APPROVED reviews, newest first."
    )
    @GetMapping("/product/{productId}")
    public ApiResponse<PagedResponse<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(
                reviewService.getApprovedReviewsForProduct(productId, pageable));
    }

    @Operation(summary = "Get average rating for a product")
    @GetMapping("/product/{productId}/rating")
    public ApiResponse<Double> getAverageRating(@PathVariable Long productId) {
        return ApiResponse.success(reviewService.getAverageRating(productId));
    }
}
