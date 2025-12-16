package com.barbershop.model.dto.auth;

import lombok.*;

/**
 * LoginRequest DTO - Request body for login endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    private String username;
    private String password;
    private Boolean rememberMe; // If true, generate refresh token
}
