package com.tappy.pos.model.dto.auth;

import lombok.*;

/**
 * RefreshTokenRequest DTO - Request body for refresh token endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class RefreshTokenRequest {
    private String refreshToken;
}
