package com.tappy.pos.model.dto.auth;

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
    private boolean setupComplete;
    private String tenantId;

    public static AuthResponse requiredAction(String username, String requiredAction, String accessToken, Long expiresIn) {
        return AuthResponse.builder()
                .username(username)
                .accessToken(accessToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(expiresIn)
                .requiredAction(requiredAction)
                .setupComplete(false)
                .build();
    }

    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn, String username, Long employeeId, boolean setupComplete, String tenantId) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenType(TOKEN_TYPE)
                .username(username)
                .employeeId(employeeId)
                .setupComplete(setupComplete)
                .tenantId(tenantId)
                .build();
    }
}
