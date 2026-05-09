package com.locnguyen.ecommerce.domains.auth.service;

import com.locnguyen.ecommerce.domains.auth.dto.ResetPasswordRequest;
import com.locnguyen.ecommerce.domains.auth.dto.ResetTokenResponse;

/**
 * Coordinates the forgot-password / verify-OTP / reset-password flow.
 */
public interface PasswordResetService {

    /**
     * Issue an OTP if the email belongs to an active account; otherwise no-op.
     * The caller (controller) MUST always return a generic success response so
     * we don't leak which emails are registered.
     */
    void requestPasswordReset(String email);

    /**
     * Validate the OTP and issue a one-shot reset token to the client.
     */
    ResetTokenResponse verifyOtpAndIssueResetToken(String email, String otp);

    /**
     * Apply the reset using the previously issued reset token. Updates the
     * password hash, marks the verification token used, revokes all active
     * refresh sessions, and writes an audit entry.
     */
    void resetPassword(ResetPasswordRequest request);
}
