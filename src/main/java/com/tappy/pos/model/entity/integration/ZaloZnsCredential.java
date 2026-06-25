package com.tappy.pos.model.entity.integration;

import com.tappy.pos.model.converter.EncryptedStringConverter;
import com.tappy.pos.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Platform-level Zalo Official Account credential used to send password-reset OTP
 * via the "Tappy Việt Nam" OA.
 *
 * <p>This is a <strong>master</strong> table (no {@code tenant_id}, no RLS) because the
 * forgot-password flow runs with no tenant context. There is exactly one logical row.
 *
 * <p>{@code appSecret}, {@code accessToken} and {@code refreshToken} are transparently
 * AES-encrypted at rest by {@link EncryptedStringConverter}. The {@code refreshToken}
 * rotates on every refresh and is persisted back here by
 * {@link com.tappy.pos.service.auth.PlatformZaloTokenService}.
 */
@Entity
@Table(name = "zalo_zns_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ZaloZnsCredential extends BaseEntity {

    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "app_secret", columnDefinition = "TEXT", nullable = false)
    private String appSecret;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "oa_name")
    private String oaName;
}
