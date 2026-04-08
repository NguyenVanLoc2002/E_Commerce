package com.locnguyen.ecommerce.domains.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Combined response for register and login — returns both user info and tokens
 * so the client has everything needed after authentication.
 */
@Getter
@Builder
@Schema(description = "Authentication response — user profile + token pair")
public class AuthResponse {

    @Schema(description = "Authenticated user profile")
    private final UserResponse user;

    @Schema(description = "JWT token pair")
    private final TokenResponse tokens;
}
