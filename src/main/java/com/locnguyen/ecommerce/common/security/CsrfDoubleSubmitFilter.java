package com.locnguyen.ecommerce.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locnguyen.ecommerce.common.config.AppProperties;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

/**
 * Double-submit-cookie CSRF protection for cookie-mutating endpoints.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>For GET/HEAD/OPTIONS or any method on non-protected paths: lets the
 *       request through unchanged.</li>
 *   <li>On every request: ensures an {@code XSRF-TOKEN} cookie is present
 *       (issuing a fresh value if missing). The cookie is non-HttpOnly so the
 *       front-end JS can read it and echo the value in the {@code X-XSRF-TOKEN}
 *       header.</li>
 *   <li>For POST/PUT/PATCH/DELETE on protected paths
 *       ({@code /api/v1/auth/refresh-token} and {@code /api/v1/auth/logout}):
 *       requires the {@code X-XSRF-TOKEN} header to match the {@code XSRF-TOKEN}
 *       cookie value. Mismatch / missing → 403.</li>
 * </ul>
 *
 * <p>Globally enabled / disabled via {@code app.security.csrf-double-submit-enabled}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsrfDoubleSubmitFilter extends OncePerRequestFilter {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/logout"
    );
    private static final Set<String> MUTATION_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!appProperties.getSecurity().isCsrfDoubleSubmitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String existingCookieValue = readCsrfCookie(request);
        if (!StringUtils.hasText(existingCookieValue)) {
            existingCookieValue = issueFreshCsrfCookie(response);
        }

        if (mustValidate(request)) {
            String headerValue = request.getHeader(CSRF_HEADER_NAME);
            if (!StringUtils.hasText(headerValue) || !headerValue.equals(existingCookieValue)) {
                log.warn("CSRF rejected: path={} method={} headerPresent={} cookiePresent={}",
                        request.getRequestURI(), request.getMethod(),
                        StringUtils.hasText(headerValue),
                        StringUtils.hasText(existingCookieValue));
                writeForbidden(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean mustValidate(HttpServletRequest request) {
        return PROTECTED_PATHS.contains(request.getRequestURI())
                && MUTATION_METHODS.contains(request.getMethod());
    }

    private String readCsrfCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (CSRF_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String issueFreshCsrfCookie(HttpServletResponse response) {
        byte[] buffer = new byte[32];
        RANDOM.nextBytes(buffer);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);

        ResponseCookie cookie = ResponseCookie.from(CSRF_COOKIE_NAME, value)
                .httpOnly(false)
                .secure(appProperties.getAuth().getRefreshCookie().isSecure())
                .sameSite(appProperties.getAuth().getRefreshCookie().getSameSite())
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return value;
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse body = ErrorResponse.of(ErrorCode.CSRF_TOKEN_INVALID, request.getRequestURI());
        response.setStatus(ErrorCode.CSRF_TOKEN_INVALID.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
