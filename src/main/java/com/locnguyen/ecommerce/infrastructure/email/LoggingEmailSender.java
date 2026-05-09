package com.locnguyen.ecommerce.infrastructure.email;

import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Default {@link EmailSender} for environments without an SMTP provider.
 *
 * <p>Records that an OTP email <em>would have been sent</em>; never logs the
 * raw OTP. Replaced automatically when another {@link EmailSender} bean
 * (real provider) is registered in the application context.
 */
@Slf4j
@Component
@Primary
@ConditionalOnMissingBean(name = "smtpEmailSender")
public class LoggingEmailSender implements EmailSender {

    @Override
    public void sendOtpEmail(String toEmail, String otp, VerificationPurpose purpose, int expiresMinutes) {
        // Never log the raw OTP — this is the safe default for the dev profile.
        log.info("[EMAIL-DEV] OTP email would be sent to: {} for purpose: {} (expires in {} min)",
                toEmail, purpose, expiresMinutes);
    }
}
