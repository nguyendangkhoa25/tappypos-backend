package com.knp.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChangePasswordRequest DTO
 * Used for password change operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    /**
     * Current password (required for regular password change)
     */
    private String oldPassword;

    /**
     * New password (required for all password changes)
     */
    private String newPassword;

    /**
     * Confirm password (optional - validation can be done on frontend)
     */
    private String confirmPassword;
}

