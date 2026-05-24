package com.tappy.pos.model.entity.tenant;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit record created whenever a shop owner deletes their shop.
 * Stored in the master DB (no tenant isolation needed).
 */
@Entity
@Table(name = "shop_deletion_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopDeletionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(name = "deleted_by", nullable = false, length = 100)
    private String deletedBy;

    @Builder.Default
    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt = LocalDateTime.now();

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Column(name = "user_count", nullable = false)
    private int userCount = 0;
}
