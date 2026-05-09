package com.locnguyen.ecommerce.domains.auth.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.auth.dto.ChangePasswordRequest;
import com.locnguyen.ecommerce.domains.auth.service.ChangePasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated account-management endpoints.
 *
 * <p>Anything here REQUIRES a valid Bearer access token. Public flows
 * (forgot/reset) live on {@link AuthController}.
 */
@Tag(name = "Account", description = "Authenticated account management")
@RestController
@RequestMapping(AppConstants.API_V1 + "/account")
@RequiredArgsConstructor
public class AccountController {

    private final ChangePasswordService changePasswordService;

    @Operation(
            summary = "Change password (authenticated)",
            description = "Verifies the current password, applies password policy, " +
                    "and revokes all active refresh sessions on success.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        changePasswordService.changePassword(resolveAuthenticatedEmail(), request);
        return ApiResponse.noContent();
    }

    private String resolveAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
