package com.locnguyen.ecommerce.domains.auth.service.impl;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.security.AuthPrincipalType;
import com.locnguyen.ecommerce.common.validation.PasswordPolicyValidator;
import com.locnguyen.ecommerce.domains.auditlog.enums.AuditAction;
import com.locnguyen.ecommerce.domains.auditlog.service.AuditLogService;
import com.locnguyen.ecommerce.domains.auth.dto.ResetPasswordRequest;
import com.locnguyen.ecommerce.domains.auth.dto.ResetTokenResponse;
import com.locnguyen.ecommerce.domains.auth.service.AuthService;
import com.locnguyen.ecommerce.domains.auth.service.PasswordResetService;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.customer.repository.CustomerRepository;
import com.locnguyen.ecommerce.domains.user.entity.User;
import com.locnguyen.ecommerce.domains.user.enums.RoleName;
import com.locnguyen.ecommerce.domains.user.enums.UserStatus;
import com.locnguyen.ecommerce.domains.user.repository.UserRepository;
import com.locnguyen.ecommerce.domains.verification.entity.VerificationToken;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import com.locnguyen.ecommerce.domains.verification.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = normalize(email);

        // Always rate-limit/audit by target, but only issue OTP for an active user.
        // Generic 200 is enforced by the controller regardless of branch.
        userRepository.findByEmailAndDeletedFalse(normalized).ifPresentOrElse(
                user -> {
                    if (user.getStatus() != UserStatus.ACTIVE) {
                        log.info("Forgot-password ignored (user inactive): email={}", normalized);
                        return;
                    }
                    otpService.generateAndSendOtp(normalized, VerificationPurpose.FORGOT_PASSWORD);
                    auditLogService.log(AuditAction.PASSWORD_RESET_REQUESTED, "USER",
                            String.valueOf(user.getId()), "email=" + normalized);
                },
                () -> log.info("Forgot-password ignored (no such user): email={}", normalized)
        );
    }

    @Override
    @Transactional
    public ResetTokenResponse verifyOtpAndIssueResetToken(String email, String otp) {
        String normalized = normalize(email);
        VerificationToken verified = otpService.verifyOtp(normalized,
                VerificationPurpose.FORGOT_PASSWORD, otp);
        OtpService.IssuedResetToken issued = otpService.issueResetToken(verified);
        return ResetTokenResponse.builder()
                .resetToken(issued.rawResetToken())
                .expiresAt(issued.expiresAt())
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }
        passwordPolicyValidator.validate(request.getNewPassword());

        VerificationToken token = otpService.validateResetToken(request.getResetToken());
        if (token.getPurpose() != VerificationPurpose.FORGOT_PASSWORD) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        }

        User user = userRepository.findByEmailAndDeletedFalse(token.getTarget())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_REUSED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.markUsed(token);
        revokeAllSessions(user);

        auditLogService.log(AuditAction.PASSWORD_RESET_SUCCESS, "USER",
                String.valueOf(user.getId()), "email=" + user.getEmail());
        log.info("Password reset completed: userId={} email={}", user.getId(), user.getEmail());
    }

    private void revokeAllSessions(User user) {
        boolean customerOnly = user.getRoles().size() == 1
                && user.getRoles().stream().allMatch(role -> role.getName() == RoleName.CUSTOMER);

        if (customerOnly) {
            UUID customerId = customerRepository.findByUserIdAndDeletedFalse(user.getId())
                    .map(Customer::getId)
                    .orElse(null);
            if (customerId != null) {
                authService.revokeAllRefreshSessions(AuthPrincipalType.CUSTOMER, customerId);
                return;
            }
        }
        authService.revokeAllRefreshSessions(AuthPrincipalType.USER, user.getId());
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
