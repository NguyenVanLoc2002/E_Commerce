package com.locnguyen.ecommerce.domains.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to submit a product review")
public class CreateReviewRequest {

    @NotNull
    @Schema(description = "ID of the order item being reviewed. " +
            "The order must be COMPLETED and belong to the current customer.")
    private Long orderItemId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "Star rating from 1 (worst) to 5 (best)")
    private Integer rating;

    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String body;
}
