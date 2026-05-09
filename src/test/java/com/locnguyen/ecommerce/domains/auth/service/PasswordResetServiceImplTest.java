package com.locnguyen.ecommerce.domains.auth.service;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.security.AuthPrincipalType;
import com.locnguyen.ecommerce.common.validation.PasswordPolicyValidator;
import com.locnguyen.ecommerce.domains.auditlog.enums.AuditAction;
import com.locnguyen.ecommerce.domains.auditlog.service.AuditLogService;
import com.locnguyen.ecommerce.domains.auth.dto.ResetPasswordRequest;
import com.locnguyen.ecommerce.domains.auth.dto.ResetTokenResponse;
import com.locnguyen.ecommerce.domains.auth.service.impl.PasswordResetServiceImpl;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.customer.repository.CustomerRepository;
import com.locnguyen.ecommerce.domains.user.entity.Role;
import com.locnguyen.ecommerce.domains.user.entity.User;
import com.locnguyen.ecommerce.domains.user.enums.RoleName;
import com.locnguyen.ecommerce.domains.user.enums.UserStatus;
import com.locnguyen.ecommerce.domains.user.repository.UserRepository;
import com.locnguyen.ecommerce.domains.verification.entity.VerificationToken;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import com.locnguyen.ecommerce.domains.verification.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock OtpService otpService;
    @Mock UserRepository userRepository;
    @Mock CustomerRepository customerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthService authService;
    @Mock AuditLogService auditLogService;

    private PasswordResetServiceImpl service;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(
                otpService,
                userRepository,
                customerRepository,
                passwordEncoder,
                new PasswordPolicyValidator(),
                authService,
                auditLogService
        );
    }

    @Test
    void requestPasswordReset_emailNotFound_isNoOp() {
        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.empty());

        service.requestPasswordReset(EMAIL);

        verify(otpService, never()).generateAndSendOtp(any(), any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void requestPasswordReset_inactiveUser_isNoOp() {
        User user = activeUser();
        user.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));

        service.requestPasswordReset(EMAIL);

        verify(otpService, never()).generateAndSendOtp(any(), any());
    }

    @Test
    void requestPasswordReset_activeUser_issuesOtp() {
        User user = activeUser();
        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));

        service.requestPasswordReset(EMAIL);

        verify(otpService).generateAndSendOtp(EMAIL, VerificationPurpose.FORGOT_PASSWORD);
        verify(auditLogService).log(eq(AuditAction.PASSWORD_RESET_REQUESTED),
                eq("USER"), eq(user.getId().toString()), any());
    }

    @Test
    void resetPassword_succeeds_revokesAllSessions() {
        VerificationToken token = new VerificationToken();
        token.setTarget(EMAIL);
        token.setPurpose(VerificationPurpose.FORGOT_PASSWORD);
        when(otpService.validateResetToken("raw-reset")).thenReturn(token);

        User user = activeUser();
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(user);
        ReflectionTestUtils.setField(customer, "id", customerId);

        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewSecret123", "ENC_old")).thenReturn(false);
        when(passwordEncoder.encode("NewSecret123")).thenReturn("ENC_new");
        when(customerRepository.findByUserIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(customer));

        ResetPasswordRequest request = new ResetPasswordRequest(
                "raw-reset", "NewSecret123", "NewSecret123");

        service.resetPassword(request);

        verify(userRepository).save(user);
        assertThat(user.getPasswordHash()).isEqualTo("ENC_new");
        verify(otpService).markUsed(token);
        verify(authService).revokeAllRefreshSessions(AuthPrincipalType.CUSTOMER, customerId);
        verify(auditLogService).log(eq(AuditAction.PASSWORD_RESET_SUCCESS),
                eq("USER"), eq(user.getId().toString()), any());
    }

    @Test
    void resetPassword_passwordMismatch_throws() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "raw-reset", "NewSecret123", "Different123");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_MISMATCH);
    }

    @Test
    void resetPassword_violatesPolicy_throws() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "raw-reset", "weakpass", "weakpass");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATED);
    }

    @Test
    void resetPassword_reusedPassword_throws() {
        VerificationToken token = new VerificationToken();
        token.setTarget(EMAIL);
        token.setPurpose(VerificationPurpose.FORGOT_PASSWORD);
        when(otpService.validateResetToken("raw-reset")).thenReturn(token);

        User user = activeUser();
        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewSecret123", "ENC_old")).thenReturn(true);

        ResetPasswordRequest request = new ResetPasswordRequest(
                "raw-reset", "NewSecret123", "NewSecret123");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_REUSED);
    }

    @Test
    void verifyOtpAndIssueResetToken_returnsTokenFromOtpService() {
        VerificationToken verified = new VerificationToken();
        verified.setTarget(EMAIL);
        verified.setPurpose(VerificationPurpose.FORGOT_PASSWORD);
        when(otpService.verifyOtp(EMAIL, VerificationPurpose.FORGOT_PASSWORD, "123456"))
                .thenReturn(verified);
        Instant exp = Instant.now().plusSeconds(600);
        when(otpService.issueResetToken(verified))
                .thenReturn(new OtpService.IssuedResetToken("opaque-reset", exp));

        ResetTokenResponse response = service.verifyOtpAndIssueResetToken(EMAIL, "123456");
        assertThat(response.getResetToken()).isEqualTo("opaque-reset");
        assertThat(response.getExpiresAt()).isEqualTo(exp);
    }

    private User activeUser() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setPasswordHash("ENC_old");
        user.setStatus(UserStatus.ACTIVE);
        Role customerRole = new Role();
        customerRole.setName(RoleName.CUSTOMER);
        user.setRoles(Set.of(customerRole));
        return user;
    }
}
