package com.knp.model.dto.auth;

import lombok.*;

/**
 * PasswordResetResponse - Response DTO for password reset operation
 * Contains the newly generated temporary password and user details
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetResponse {

    private Long userId;
    private String username;
    private String email;
    private String tempPassword;
    private String message;
    private Boolean requirePasswordChange;

}

