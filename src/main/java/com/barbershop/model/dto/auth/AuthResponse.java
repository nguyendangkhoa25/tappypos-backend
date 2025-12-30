package com.barbershop.model.dto.auth;

import lombok.*;

/**
 * AuthResponse DTO - Response for login/refresh token endpoints
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private static final String TOKEN_TYPE = "Bearer";
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private String username;
    private Long employeeId;
    private String requiredAction;

    public static AuthResponse requiredAction(String username, String requiredAction, String accessToken, Long expiresIn) {
        return AuthResponse.builder()
                .username(username)
                .accessToken(accessToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(expiresIn)
                .requiredAction(requiredAction)
                .build();
    }

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, String username, Long employeeId) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType(TOKEN_TYPE)
                .username(username)
                .employeeId(employeeId)
                .build();
    }
}
