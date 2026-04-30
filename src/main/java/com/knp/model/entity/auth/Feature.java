package com.knp.model.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import com.knp.model.entity.BaseEntity;

/**
 * Feature entity - Represents application features for RBAC
 * IMPORTANT: This entity is stored in the MASTER DATABASE ONLY
 * Features correspond to menu items and pages in the application
 * Features are mapped to roles via role_features junction table
 * All tenants share the same set of features defined in the master database
 *
 * Examples: DASHBOARD, ORDER, CUSTOMER, PRODUCT, SALARY, INVOICE, PROMOTION, EMPLOYEE, USER, REVENUE, MY_WORK, SHOP_INFO
 *
 * Features are defined via FeatureEnum with Vietnamese localization
 */
@Entity
@Table(name = "features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature extends BaseEntity {

    /**
     * Feature key - matches FeatureEnum.name() (e.g., DASHBOARD, ORDER, etc.)
     * Must be unique across the system
     */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    /**
     * Vietnamese display name (e.g., "Bảng Điều Khiển")
     * Used for UI labels and menu items
     */
    @Column(nullable = false, length = 100)
    private String displayName;

    /**
     * Vietnamese description (e.g., "Xem tổng quan và thống kê chính của cửa hàng")
     * Used for tooltips and help text
     */
    @Column(length = 500)
    private String description;

    /**
     * Whether this feature is active/visible in the system
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Soft delete flag - features are rarely deleted to maintain audit trail
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // Constructor for convenience
    public Feature(String name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.active = true;
        this.deleted = false;
    }
}

