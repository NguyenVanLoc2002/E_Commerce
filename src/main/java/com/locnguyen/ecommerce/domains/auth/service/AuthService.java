package com.locnguyen.ecommerce.domains.auth.service;

import com.locnguyen.ecommerce.domains.auth.dto.AuthResponse;
import com.locnguyen.ecommerce.domains.auth.dto.LoginRequest;
import com.locnguyen.ecommerce.domains.auth.dto.RefreshTokenRequest;
import com.locnguyen.ecommerce.domains.auth.dto.RegisterRequest;
import com.locnguyen.ecommerce.domains.auth.dto.TokenResponse;

public interface AuthService {

    void logout(String token);

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);
}
