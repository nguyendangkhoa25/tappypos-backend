package com.tappy.pos.model.entity.tenant;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.BaseEntity;
import com.tappy.pos.model.enums.ShopType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dbName;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_orders_per_month")
    private Integer maxOrdersPerMonth;

    @Column(name = "features", columnDefinition = "TEXT")
    private String features; // Comma-separated list of features

    @Column(name = "subscription_type")
    private String subscriptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shop_type", length = 30)
    private ShopType shopType;

    @Column(name = "contact_person_name")
    private String contactPersonName;

    @Column(name = "contact_person_phone")
    private String contactPersonPhone;

    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    @Column(name = "contact_person_zalo_id")
    private String contactPersonZaloId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "active_at")
    private Long activeAt;

    @Column(name = "active_by")
    private String activeBy;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Builder.Default
    @Column(name = "setup_complete", nullable = false)
    private boolean setupComplete = true;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}

