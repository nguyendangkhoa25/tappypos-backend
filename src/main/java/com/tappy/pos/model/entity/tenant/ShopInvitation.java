package com.tappy.pos.model.entity.tenant;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A one-time, short-lived invitation code that lets an existing registered user
 * join a shop with a pre-assigned role and feature set.
 *
 * This is a master-DB entity (not tenant-scoped) — it lives alongside {@code Tenant}
 * and {@code User} in the shared schema.
 */
@Entity
@Table(name = "shop_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The shop that issued this invitation. */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /** 6-character alphanumeric code shown to the invitee. */
    @Column(name = "code", nullable = false, unique = true, length = 8)
    private String code;

    /** Role to assign when the invitee accepts. */
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    /**
     * JSON array of feature keys, e.g. {@code ["ORDER","MY_WORK","POS"]}.
     * Stored as a JSONB column.
     */
    @Column(name = "features", nullable = false, columnDefinition = "jsonb")
    private String features;

    /** Username of the shop owner who generated the code. */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    /** Code is valid until this moment (created_at + 5 minutes). */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Set when the invitation is accepted; {@code null} means still pending. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Username of the user who accepted the invitation. */
    @Column(name = "used_by", length = 100)
    private String usedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── helpers ────────────────────────────────────────────────────────────

    public boolean isValid() {
        return usedAt == null && LocalDateTime.now().isBefore(expiresAt);
    }

    public long secondsRemaining() {
        if (!isValid()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }
}
