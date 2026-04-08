package com.locnguyen.ecommerce.domains.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "JWT token pair")
public class TokenResponse {

    @Schema(description = "JWT access token — short-lived, used in Authorization header", example = "eyJhbGci...")
    private final String accessToken;

    @Schema(description = "JWT refresh token — long-lived, used to obtain new access tokens", example = "eyJhbGci...")
    private final String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private final String tokenType;

    @Schema(description = "Access token expiration in seconds", example = "3600")
    private final long expiresIn;
}
