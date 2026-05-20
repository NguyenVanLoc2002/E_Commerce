package com.locnguyen.ecommerce.domains.payment.service;

import com.locnguyen.ecommerce.domains.payment.dto.MomoPaymentConfigRequest;
import com.locnguyen.ecommerce.domains.payment.dto.MomoPaymentConfigResponse;
import com.locnguyen.ecommerce.domains.payment.dto.PaypalPaymentConfigRequest;
import com.locnguyen.ecommerce.domains.payment.dto.PaypalPaymentConfigResponse;

public interface PaymentConfigService {

    MomoPaymentConfigResponse getMomoConfig();

    MomoPaymentConfigResponse updateMomoConfig(MomoPaymentConfigRequest request);

    PaypalPaymentConfigResponse getPaypalConfig();

    PaypalPaymentConfigResponse updatePaypalConfig(PaypalPaymentConfigRequest request);
}
