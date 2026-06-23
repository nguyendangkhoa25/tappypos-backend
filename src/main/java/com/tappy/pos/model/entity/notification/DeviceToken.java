package com.tappy.pos.model.entity.notification;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * An Expo push token for one user's device. Used to deliver real push notifications
 * (banner/sound when the mobile app is backgrounded). One row per device; the token
 * is globally unique so re-registration upserts onto the same row.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeviceToken extends TenantAwareEntity {

    /** Username (matches notifications.user_id). */
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "expo_push_token", nullable = false, length = 255)
    private String expoPushToken;

    @Column(name = "platform", length = 10)
    private String platform;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}
