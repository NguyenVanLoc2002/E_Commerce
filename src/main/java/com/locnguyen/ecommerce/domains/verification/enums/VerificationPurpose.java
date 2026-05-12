package com.locnguyen.ecommerce.domains.verification.enums;

/**
 * Why a verification token was issued.
 *
 * <p>The purpose value partitions the verification keyspace so that an OTP
 * issued for one flow cannot be replayed against another (e.g. a
 * {@link #FORGOT_PASSWORD} OTP must not be valid for {@link #VERIFY_EMAIL}).
 */
public enum VerificationPurpose {
    FORGOT_PASSWORD,
    CHANGE_PASSWORD,
    VERIFY_EMAIL,
    VERIFY_ORDER
}
