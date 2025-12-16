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
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private String username;

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, String username) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType("Bearer")
                .username(username)
                .build();
    }
}
