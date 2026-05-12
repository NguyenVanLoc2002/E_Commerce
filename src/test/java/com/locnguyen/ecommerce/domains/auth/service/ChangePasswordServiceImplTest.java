package com.locnguyen.ecommerce.domains.auth.service;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.security.AuthPrincipalType;
import com.locnguyen.ecommerce.common.validation.PasswordPolicyValidator;
import com.locnguyen.ecommerce.domains.auditlog.enums.AuditAction;
import com.locnguyen.ecommerce.domains.auditlog.service.AuditLogService;
import com.locnguyen.ecommerce.domains.auth.dto.ChangePasswordRequest;
import com.locnguyen.ecommerce.domains.auth.service.impl.ChangePasswordServiceImpl;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.customer.repository.CustomerRepository;
import com.locnguyen.ecommerce.domains.user.entity.Role;
import com.locnguyen.ecommerce.domains.user.entity.User;
import com.locnguyen.ecommerce.domains.user.enums.RoleName;
import com.locnguyen.ecommerce.domains.user.enums.UserStatus;
import com.locnguyen.ecommerce.domains.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock CustomerRepository customerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthService authService;
    @Mock AuditLogService auditLogService;

    private ChangePasswordServiceImpl service;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        service = new ChangePasswordServiceImpl(
                userRepository,
                customerRepository,
                passwordEncoder,
                new PasswordPolicyValidator(),
                authService,
                auditLogService
        );
    }

    @Test
    void changePassword_wrongCurrentPassword_throws422AndAudits() {
        User user = activeUser();
        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", "ENC_old")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPass1", "NewSecret123", "NewSecret123");

        assertThatThrownBy(() -> service.changePassword(EMAIL, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_PASSWORD_INVALID);

        verify(authService, never()).revokeAllRefreshSessions(any(), any());
        verify(auditLogService).log(eq(AuditAction.LOGIN_FAILURE),
                eq("USER"), eq(user.getId().toString()), any());
    }

    @Test
    void changePassword_success_revokesAllSessions() {
        User user = activeUser();
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(user);
        ReflectionTestUtils.setField(customer, "id", customerId);

        when(userRepository.findByEmailAndDeletedFalse(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CurrentPass1", "ENC_old")).thenReturn(true);
        when(passwordEncoder.matches("NewSecret123", "ENC_old")).thenReturn(false);
        when(passwordEncoder.encode("NewSecret123")).thenReturn("ENC_new");
        when(customerRepository.findByUserIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(customer));

        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass1", "NewSecret123", "NewSecret123");

        service.changePassword(EMAIL, request);

        assertThat(user.getPasswordHash()).isEqualTo("ENC_new");
        verify(userRepository).save(user);
        verify(authService).revokeAllRefreshSessions(AuthPrincipalType.CUSTOMER, customerId);
        verify(auditLogService).log(eq(AuditAction.PASSWORD_CHANGE_SUCCESS),
                eq("USER"), eq(user.getId().toString()), any());
    }

    @Test
    void changePassword_passwordMismatch_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass1", "NewSecret123", "Different456");

        assertThatThrownBy(() -> service.changePassword(EMAIL, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_MISMATCH);
    }

    @Test
    void changePassword_policyViolated_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass1", "weak", "weak");

        assertThatThrownBy(() -> service.changePassword(EMAIL, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATED);
    }

    @Test
    void changePassword_unauthenticated_throws() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass1", "NewSecret123", "NewSecret123");
        assertThatThrownBy(() -> service.changePassword(null, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
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
