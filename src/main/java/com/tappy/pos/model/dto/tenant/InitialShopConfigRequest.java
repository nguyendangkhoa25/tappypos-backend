package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional initial shop_config values to seed when creating a new tenant.
 * Only fields present in the selected feature set are applied; absent/null fields
 * fall back to the system defaults seeded by TenantProvisioningService.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitialShopConfigRequest {

    /** POS mode: "STANDARD", "TABLE", or "SERVICE". Requires POS feature. */
    private String posMode;

    /**
     * Pawn category config JSON string.
     * Shape: {"enabled":["GOLD","ELECTRONICS"],"default":"GOLD","showPicker":true}
     * Requires PAWN feature.
     */
    private String pawnCategoryConfig;
}
