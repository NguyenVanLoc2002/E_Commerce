package com.locnguyen.ecommerce.domains.verification.service;

import com.locnguyen.ecommerce.common.config.AppProperties;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.user.repository.UserRepository;
import com.locnguyen.ecommerce.domains.verification.entity.VerificationToken;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import com.locnguyen.ecommerce.domains.verification.repository.VerificationTokenRepository;
import com.locnguyen.ecommerce.domains.verification.service.impl.OtpServiceImpl;
import com.locnguyen.ecommerce.infrastructure.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OtpServiceImpl}.
 *
 * <p>Verifies the most security-critical invariants:
 * <ul>
 *   <li>OTP is stored as a hash (never plaintext).</li>
 *   <li>Expired OTP fails with {@link ErrorCode#OTP_EXPIRED}.</li>
 *   <li>Already-used OTP fails with {@link ErrorCode#OTP_USED}.</li>
 *   <li>Max attempts exceeded fails with {@link ErrorCode#OTP_TOO_MANY_ATTEMPTS}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock UserRepository userRepository;
    @Mock OtpRateLimiter otpRateLimiter;
    @Mock EmailSender emailSender;

    private OtpServiceImpl otpService;
    private AppProperties appProperties;

    private static final String EMAIL = "user@example.com";
    private static final VerificationPurpose PURPOSE = VerificationPurpose.FORGOT_PASSWORD;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getJwt().setSecret("test-pepper-secret-32-bytes-minimum");

        otpService = new OtpServiceImpl(
                verificationTokenRepository,
                userRepository,
                otpRateLimiter,
                emailSender,
                appProperties
        );
    }

    @Test
    void generateAndSendOtp_storesHashNotPlaintext() {
        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.empty());
        when(verificationTokenRepository.markPreviousSuperseded(eq(EMAIL), eq(PURPOSE), any())).thenReturn(0);
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailSender).sendOtpEmail(eq(EMAIL), anyString(), eq(PURPOSE), any(int.class));

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        ArgumentCaptor<String> rawOtpCaptor = ArgumentCaptor.forClass(String.class);

        VerificationToken result = otpService.generateAndSendOtp(EMAIL, PURPOSE);

        verify(verificationTokenRepository).save(captor.capture());
        verify(emailSender).sendOtpEmail(eq(EMAIL), rawOtpCaptor.capture(), eq(PURPOSE), any(int.class));

        VerificationToken saved = captor.getValue();
        String rawOtpSent = rawOtpCaptor.getValue();

        assertThat(rawOtpSent).hasSize(6).matches("\\d{6}");
        assertThat(saved.getTokenHash())
                .as("OTP must be stored as a hash, never as plaintext")
                .isNotNull()
                .isNotEqualTo(rawOtpSent)
                .doesNotContain(rawOtpSent);
        assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex
        assertThat(saved.getTarget()).isEqualTo(EMAIL);
        assertThat(saved.getPurpose()).isEqualTo(PURPOSE);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void verifyOtp_rejectsExpiredToken() {
        VerificationToken token = activeTokenWithHash(otpHashFor("123456"));
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        // Repository returns only active (non-expired) — to simulate the expired path
        // we instead return a token that the active query found but whose state changed.
        when(verificationTokenRepository.findActiveByTargetAndPurpose(eq(EMAIL), eq(PURPOSE), any()))
                .thenReturn(List.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, PURPOSE, "123456"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OTP_EXPIRED);
    }

    @Test
    void verifyOtp_rejectsUsedToken() {
        VerificationToken token = activeTokenWithHash(otpHashFor("123456"));
        token.setUsedAt(LocalDateTime.now().minusSeconds(5));
        when(verificationTokenRepository.findActiveByTargetAndPurpose(eq(EMAIL), eq(PURPOSE), any()))
                .thenReturn(List.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, PURPOSE, "123456"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OTP_USED);
    }

    @Test
    void verifyOtp_rejectsAfterMaxAttempts() {
        VerificationToken token = activeTokenWithHash(otpHashFor("999999"));
        token.setMaxAttempts(5);
        token.setAttemptCount(5);
        when(verificationTokenRepository.findActiveByTargetAndPurpose(eq(EMAIL), eq(PURPOSE), any()))
                .thenReturn(List.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, PURPOSE, "000000"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
    }

    @Test
    void verifyOtp_rejectsWrongOtpAndIncrementsAttemptCount() {
        VerificationToken token = activeTokenWithHash(otpHashFor("111111"));
        token.setAttemptCount(0);
        token.setMaxAttempts(5);
        when(verificationTokenRepository.findActiveByTargetAndPurpose(eq(EMAIL), eq(PURPOSE), any()))
                .thenReturn(List.of(token));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, PURPOSE, "222222"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OTP_INVALID);
        assertThat(token.getAttemptCount()).isEqualTo(1);
        verify(verificationTokenRepository).save(token);
    }

    @Test
    void verifyOtp_acceptsCorrectOtpAndMarksVerified() {
        VerificationToken token = activeTokenWithHash(otpHashFor("424242"));
        token.setAttemptCount(0);
        when(verificationTokenRepository.findActiveByTargetAndPurpose(eq(EMAIL), eq(PURPOSE), any()))
                .thenReturn(List.of(token));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        VerificationToken result = otpService.verifyOtp(EMAIL, PURPOSE, "424242");

        assertThat(result.getVerifiedAt()).isNotNull();
        assertThat(result.getAttemptCount()).isEqualTo(1);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private VerificationToken activeTokenWithHash(String hash) {
        VerificationToken token = new VerificationToken();
        token.setTarget(EMAIL);
        token.setPurpose(PURPOSE);
        token.setTokenHash(hash);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setMaxAttempts(5);
        return token;
    }

    /**
     * Compute the same hash the production code stores. Mirrors
     * {@link OtpServiceImpl} — kept here so tests are independent of any
     * package-private API change.
     */
    private String otpHashFor(String rawOtp) {
        try {
            String pepper = appProperties.getJwt().getSecret();
            String input = PURPOSE.name() + ":" + EMAIL + ":" + rawOtp + ":" + pepper;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of()
                    .formatHex(digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
