package com.locnguyen.ecommerce.domains.auth.service.impl;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.security.AuthPrincipalType;
import com.locnguyen.ecommerce.common.validation.PasswordPolicyValidator;
import com.locnguyen.ecommerce.domains.auditlog.enums.AuditAction;
import com.locnguyen.ecommerce.domains.auditlog.service.AuditLogService;
import com.locnguyen.ecommerce.domains.auth.dto.ChangePasswordRequest;
import com.locnguyen.ecommerce.domains.auth.service.AuthService;
import com.locnguyen.ecommerce.domains.auth.service.ChangePasswordService;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.customer.repository.CustomerRepository;
import com.locnguyen.ecommerce.domains.user.entity.User;
import com.locnguyen.ecommerce.domains.user.enums.RoleName;
import com.locnguyen.ecommerce.domains.user.enums.UserStatus;
import com.locnguyen.ecommerce.domains.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordServiceImpl implements ChangePasswordService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void changePassword(String authenticatedEmail, ChangePasswordRequest request) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }
        passwordPolicyValidator.validate(request.getNewPassword());

        User user = userRepository.findByEmailAndDeletedFalse(authenticatedEmail.toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            auditLogService.log(AuditAction.LOGIN_FAILURE, "USER",
                    String.valueOf(user.getId()),
                    "context=CHANGE_PASSWORD reason=CURRENT_PASSWORD_INVALID");
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INVALID);
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new AppException(ErrorCode.PASSWORD_REUSED);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_REUSED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        revokeAllSessions(user);

        auditLogService.log(AuditAction.PASSWORD_CHANGE_SUCCESS, "USER",
                String.valueOf(user.getId()), "email=" + user.getEmail());
        log.info("Password changed: userId={} email={}", user.getId(), user.getEmail());
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
}
