package com.locnguyen.ecommerce.domains.review.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReviewFilter {
    private Long productId;
    private Long customerId;
    private String status;
    private Integer minRating;
    private Integer maxRating;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
