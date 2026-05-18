package com.tappy.pos.model.dto.auth;

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
    private Boolean rememberMe;
    private String turnstileToken;
    private Boolean refreshInBody;
}
