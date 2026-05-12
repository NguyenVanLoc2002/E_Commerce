package com.locnguyen.ecommerce.domains.verification.service;

import com.locnguyen.ecommerce.common.config.AppProperties;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed rate limiter for OTP send/verify operations.
 *
 * <p>Three independent counters per (purpose, target):
 * <ul>
 *   <li>{@code otp:send:cooldown:*} — short cooldown after each send (TTL = resend cooldown).</li>
 *   <li>{@code otp:send:window:*} — total sends in the rolling window (TTL = window minutes).</li>
 *   <li>{@code otp:verify:window:*} — verify attempts (TTL = 15 min, used as belt-and-braces
 *       on top of per-token attempt_count).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRateLimiter {

    private static final String SEND_COOLDOWN_PREFIX = "otp:send:cooldown:";
    private static final String SEND_WINDOW_PREFIX = "otp:send:window:";
    private static final String VERIFY_WINDOW_PREFIX = "otp:verify:window:";
    private static final Duration VERIFY_WINDOW_TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;

    /**
     * Atomically check-and-record an OTP send. Throws {@link AppException} with
     * {@link ErrorCode#OTP_RATE_LIMITED} if the caller is in cooldown or has
     * exceeded the window limit.
     */
    public void checkAndRecordSend(VerificationPurpose purpose, String target) {
        String cooldownKey = SEND_COOLDOWN_PREFIX + purpose + ":" + target;
        String windowKey = SEND_WINDOW_PREFIX + purpose + ":" + target;

        AppProperties.Otp otpConfig = appProperties.getOtp();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            log.warn("OTP send rejected (cooldown active): purpose={} target={}", purpose, target);
            throw new AppException(ErrorCode.OTP_RATE_LIMITED);
        }

        Long windowCount = redisTemplate.opsForValue().increment(windowKey);
        if (windowCount != null && windowCount == 1L) {
            redisTemplate.expire(windowKey, Duration.ofMinutes(otpConfig.getSendLimitWindowMinutes()));
        }
        if (windowCount != null && windowCount > otpConfig.getSendLimitMax()) {
            log.warn("OTP send rejected (window limit exceeded): purpose={} target={} count={}",
                    purpose, target, windowCount);
            throw new AppException(ErrorCode.OTP_RATE_LIMITED);
        }

        redisTemplate.opsForValue().set(cooldownKey, "1",
                Duration.ofSeconds(otpConfig.getResendCooldownSeconds()));
    }

    /** Returns {@code true} if a cooldown window is currently active for the target. */
    public boolean checkSendCooldown(VerificationPurpose purpose, String target) {
        String cooldownKey = SEND_COOLDOWN_PREFIX + purpose + ":" + target;
        return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));
    }

    /** Records one verify attempt in the rolling 15-minute window. */
    public void recordVerifyAttempt(VerificationPurpose purpose, String target) {
        String key = VERIFY_WINDOW_PREFIX + purpose + ":" + target;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, VERIFY_WINDOW_TTL);
        }
    }
}
