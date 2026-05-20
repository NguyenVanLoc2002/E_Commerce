package com.locnguyen.ecommerce.domains.payment.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomoPaymentConfigRequest {

    private Boolean enabled;
    private String environment;
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String createUrl;
    private String redirectUrl;
    private String ipnUrl;
    private String requestType;
    private String lang;

    @Min(value = 1, message = "connectTimeoutMs must be greater than 0")
    private Integer connectTimeoutMs;

    @Min(value = 1, message = "readTimeoutMs must be greater than 0")
    private Integer readTimeoutMs;
}
