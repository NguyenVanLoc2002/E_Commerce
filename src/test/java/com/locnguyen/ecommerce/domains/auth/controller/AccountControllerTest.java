package com.locnguyen.ecommerce.domains.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.exception.GlobalExceptionHandler;
import com.locnguyen.ecommerce.domains.auth.dto.ChangePasswordRequest;
import com.locnguyen.ecommerce.domains.auth.service.ChangePasswordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock ChangePasswordService changePasswordService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AccountController controller = new AccountController(changePasswordService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePassword_authenticated_success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        ChangePasswordRequest request = new ChangePasswordRequest(
                "Current123", "NewSecret123", "NewSecret123");
        doNothing().when(changePasswordService)
                .changePassword(eq("user@example.com"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(changePasswordService).changePassword(eq("user@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_currentPasswordWrong_returns422() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPass1", "NewSecret123", "NewSecret123");
        doThrow(new AppException(ErrorCode.CURRENT_PASSWORD_INVALID))
                .when(changePasswordService).changePassword(any(), any());

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INVALID"));
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        // No SecurityContext authentication set.
        ChangePasswordRequest request = new ChangePasswordRequest(
                "Current123", "NewSecret123", "NewSecret123");

        mockMvc.perform(post("/api/v1/account/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
