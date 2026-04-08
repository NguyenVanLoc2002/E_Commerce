package com.locnguyen.ecommerce.domains.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Initiate online payment request")
public class InitPaymentRequest {

    @Schema(example = "cod", description = "Payment provider identifier (for future gateway integration)")
    private String provider;

    @Schema(example = "https://example.com/payment/callback", description = "Return URL after payment (for future redirect)")
    private String returnUrl;
}
