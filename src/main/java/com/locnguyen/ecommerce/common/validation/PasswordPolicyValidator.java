package com.locnguyen.ecommerce.common.validation;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Server-side enforcement of the application password policy.
 *
 * <p>Policy: minimum 8 characters, must contain at least one uppercase letter,
 * one lowercase letter, and one digit.
 */
@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;

    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new AppException(ErrorCode.PASSWORD_POLICY_VIOLATED,
                    "Password must be at least " + MIN_LENGTH + " characters long");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new AppException(ErrorCode.PASSWORD_POLICY_VIOLATED,
                    "Password must contain uppercase, lowercase, and digit characters");
        }
    }
}
