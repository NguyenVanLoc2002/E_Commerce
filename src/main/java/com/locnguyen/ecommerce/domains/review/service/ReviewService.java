package com.locnguyen.ecommerce.domains.review.service;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.notification.enums.NotificationType;
import com.locnguyen.ecommerce.domains.notification.service.NotificationService;
import com.locnguyen.ecommerce.domains.order.entity.Order;
import com.locnguyen.ecommerce.domains.order.entity.OrderItem;
import com.locnguyen.ecommerce.domains.order.enums.OrderStatus;
import com.locnguyen.ecommerce.domains.order.repository.OrderItemRepository;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import com.locnguyen.ecommerce.domains.review.dto.*;
import com.locnguyen.ecommerce.domains.review.entity.Review;
import com.locnguyen.ecommerce.domains.review.enums.ReviewStatus;
import com.locnguyen.ecommerce.domains.review.mapper.ReviewMapper;
import com.locnguyen.ecommerce.domains.review.repository.ReviewRepository;
import com.locnguyen.ecommerce.domains.review.specification.ReviewSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewMapper reviewMapper;
    private final NotificationService notificationService;

    // ─── Customer: submit review ──────────────────────────────────────────────

    /**
     * Submit a verified-purchase review.
     *
     * <p>Eligibility checks (in order):
     * <ol>
     *   <li>Order item must exist</li>
     *   <li>Order must belong to the current customer</li>
     *   <li>Order must be {@code COMPLETED}</li>
     *   <li>No existing review for this customer × product</li>
     * </ol>
     *
     * <p>New reviews start as {@link ReviewStatus#PENDING} awaiting admin moderation.
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, Customer customer) {
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        Order order = orderItem.getOrder();

        // Ownership check — hide existence from other customers
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        // Completed-order rule
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }

        Product product = orderItem.getVariant().getProduct();

        // One review per customer per product
        if (reviewRepository.existsByCustomerIdAndProductId(customer.getId(), product.getId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setCustomer(customer);
        review.setProduct(product);
        review.setOrderItem(orderItem);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setBody(request.getBody());

        review = reviewRepository.save(review);

        log.info("Review submitted: id={} customerId={} productId={} rating={}",
                review.getId(), customer.getId(), product.getId(), review.getRating());
        return reviewMapper.toOwnerResponse(review);
    }

    // ─── Customer: read own reviews ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getMyReviews(Customer customer, Pageable pageable) {
        Page<Review> page = reviewRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
        return PagedResponse.of(page.map(reviewMapper::toOwnerResponse));
    }

    // ─── Public: product reviews ──────────────────────────────────────────────

    /**
     * Returns only {@link ReviewStatus#APPROVED} reviews for a product, newest first.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getApprovedReviewsForProduct(Long productId,
                                                                       Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, ReviewStatus.APPROVED, pageable);
        return PagedResponse.of(page.map(reviewMapper::toPublicResponse));
    }

    /**
     * Returns the average star rating for a product (approved reviews only).
     */
    @Transactional(readOnly = true)
    public double getAverageRating(Long productId) {
        return reviewRepository.findAverageRatingByProductId(productId);
    }

    // ─── Admin: moderation ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> listReviews(ReviewFilter filter, Pageable pageable) {
        Page<Review> page = reviewRepository.findAll(
                ReviewSpecification.withFilter(filter), pageable);
        return PagedResponse.of(page.map(reviewMapper::toAdminResponse));
    }

    @Transactional(readOnly = true)
    public ReviewResponse adminGetById(Long reviewId) {
        return reviewMapper.toAdminResponse(findByIdOrThrow(reviewId));
    }

    /**
     * Approve or reject a review, then notify the customer.
     *
     * <p>Allowed transitions from {@link ReviewStatus#PENDING}:
     * <ul>
     *   <li>PENDING → APPROVED (published)</li>
     *   <li>PENDING → REJECTED (hidden)</li>
     * </ul>
     * APPROVED and REJECTED are terminal — no further moderation changes allowed.
     */
    @Transactional
    public ReviewResponse moderateReview(Long reviewId, UpdateReviewStatusRequest request) {
        Review review = findByIdOrThrow(reviewId);

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_MODERATED,
                    "Review has already been moderated: " + review.getStatus());
        }

        ReviewStatus target = request.getStatus();
        if (target != ReviewStatus.APPROVED && target != ReviewStatus.REJECTED) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Review can only be set to APPROVED or REJECTED");
        }

        review.setStatus(target);
        if (request.getAdminNote() != null) {
            review.setAdminNote(request.getAdminNote());
        }
        reviewRepository.save(review);

        // Notify customer of the moderation outcome
        notifyModerationResult(review);

        log.info("Review moderated: id={} → {} by admin", reviewId, target);
        return reviewMapper.toAdminResponse(review);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private Review findByIdOrThrow(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private void notifyModerationResult(Review review) {
        Long customerId = review.getCustomer().getId();
        String productName = review.getProduct().getName();
        String reviewId = review.getId().toString();

        if (review.getStatus() == ReviewStatus.APPROVED) {
            notificationService.send(customerId,
                    NotificationType.REVIEW_APPROVED,
                    "Your review was approved",
                    "Your review for \"" + productName + "\" has been published.",
                    "REVIEW", reviewId);
        } else {
            notificationService.send(customerId,
                    NotificationType.REVIEW_REJECTED,
                    "Your review was not approved",
                    "Your review for \"" + productName + "\" did not meet our community guidelines.",
                    "REVIEW", reviewId);
        }
    }
}
