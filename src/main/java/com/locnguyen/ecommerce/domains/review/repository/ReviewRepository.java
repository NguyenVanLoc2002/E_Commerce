package com.locnguyen.ecommerce.domains.review.repository;

import com.locnguyen.ecommerce.domains.review.entity.Review;
import com.locnguyen.ecommerce.domains.review.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>,
        JpaSpecificationExecutor<Review> {

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    Page<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId, ReviewStatus status, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r " +
           "WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r " +
           "WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    long countApprovedByProductId(@Param("productId") Long productId);
}
