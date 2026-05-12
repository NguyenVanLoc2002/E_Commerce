package com.locnguyen.ecommerce.domains.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.locnguyen.ecommerce.common.config.AppProperties;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.exception.GlobalExceptionHandler;
import com.locnguyen.ecommerce.common.security.RefreshTokenCookieService;
import com.locnguyen.ecommerce.domains.auth.dto.ForgotPasswordRequest;
import com.locnguyen.ecommerce.domains.auth.dto.ResetPasswordRequest;
import com.locnguyen.ecommerce.domains.auth.dto.ResetTokenResponse;
import com.locnguyen.ecommerce.domains.auth.dto.VerifyOtpRequest;
import com.locnguyen.ecommerce.domains.auth.service.AuthService;
import com.locnguyen.ecommerce.domains.auth.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for the public password-reset endpoints on {@link AuthController}.
 *
 * <p>Most importantly: {@code POST /api/v1/auth/password/forgot} MUST always
 * return 200, regardless of whether the email exists. The internal service
 * decides whether to actually send an OTP — the wire response cannot leak it.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock AuthService authService;
    @Mock PasswordResetService passwordResetService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getAuth().getRefreshCookie().setName("fashion-shop.refresh-token");
        appProperties.getAuth().getRefreshCookie().setPath("/api/v1/auth");

        AuthController controller = new AuthController(
                authService,
                new RefreshTokenCookieService(appProperties),
                passwordResetService
        );

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void forgotPassword_existingEmail_returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");
        doNothing().when(passwordResetService).requestPasswordReset("user@example.com");

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(passwordResetService).requestPasswordReset("user@example.com");
    }

    @Test
    void forgotPassword_unknownEmail_alsoReturns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("missing@example.com");
        // Service is a no-op for unknown emails — but the wire shape is identical.
        doNothing().when(passwordResetService).requestPasswordReset("missing@example.com");

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyForgotOtp_success_returnsResetToken() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("user@example.com", "123456");
        when(passwordResetService.verifyOtpAndIssueResetToken("user@example.com", "123456"))
                .thenReturn(ResetTokenResponse.builder()
                        .resetToken("opaque-reset")
                        .expiresAt(Instant.parse("2030-01-01T00:00:00Z"))
                        .build());

        mockMvc.perform(post("/api/v1/auth/password/forgot/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetToken").value("opaque-reset"));
    }

    @Test
    void verifyForgotOtp_invalidOtp_returns422() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("user@example.com", "000000");
        when(passwordResetService.verifyOtpAndIssueResetToken("user@example.com", "000000"))
                .thenThrow(new AppException(ErrorCode.OTP_INVALID));

        mockMvc.perform(post("/api/v1/auth/password/forgot/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"));
    }

    @Test
    void resetPassword_success_returns200() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "raw-reset", "NewSecret123", "NewSecret123");
        doNothing().when(passwordResetService).resetPassword(any());

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resetPassword_invalidToken_returns422() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "bad", "NewSecret123", "NewSecret123");
        doThrow(new AppException(ErrorCode.RESET_TOKEN_INVALID))
                .when(passwordResetService).resetPassword(any());

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }
}
