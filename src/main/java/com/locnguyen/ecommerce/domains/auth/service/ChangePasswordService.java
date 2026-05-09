package com.locnguyen.ecommerce.domains.auth.service;

import com.locnguyen.ecommerce.domains.auth.dto.ChangePasswordRequest;

/**
 * Authenticated change-password flow for the currently logged-in user.
 */
public interface ChangePasswordService {

    /**
     * Verify the current password, validate the new password, persist the new
     * hash, revoke all refresh sessions for the user, and write an audit entry.
     *
     * @param authenticatedEmail email of the user attached to the SecurityContext
     * @param request            request DTO from the controller
     */
    void changePassword(String authenticatedEmail, ChangePasswordRequest request);
}
