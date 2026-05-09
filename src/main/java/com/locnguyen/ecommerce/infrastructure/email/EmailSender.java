package com.locnguyen.ecommerce.infrastructure.email;

import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;

/**
 * Outbound transactional email gateway.
 *
 * <p>Implementations must never log raw OTP values or any plaintext secret.
 * Production implementations should integrate with a real provider (SES,
 * SendGrid, etc.) — the dev profile uses a logging-only implementation.
 */
public interface EmailSender {

    /**
     * Send an OTP email for the given purpose.
     *
     * @param toEmail        recipient email address
     * @param otp            plaintext OTP value (never logged by the implementation)
     * @param purpose        what the OTP is for (FORGOT_PASSWORD, CHANGE_PASSWORD, ...)
     * @param expiresMinutes how many minutes from now the OTP expires
     */
    void sendOtpEmail(String toEmail, String otp, VerificationPurpose purpose, int expiresMinutes);
}
