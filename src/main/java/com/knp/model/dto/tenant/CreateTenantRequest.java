package com.knp.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.knp.model.enums.ShopType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * CreateTenantRequest - Request DTO for creating a new tenant
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTenantRequest {

    private String tenantId;

    private String name;

    private String dbName;

    private LocalDate expirationDate;

    private Integer maxUsers;

    private List<String> features;

    private String subscriptionType;

    @NotNull(message = "Shop type is required")
    private ShopType shopType;

    private String shopAddress;

    private String contactPersonName;

    private String contactPersonPhone;

    private String contactPersonEmail;

    private String contactPersonZaloId;

    @NotBlank(message = "Admin username is required")
    private String adminUsername;

    @NotBlank(message = "Admin password is required")
    private String adminPassword;

    /** Role/feature assignments for this shop. Null = use system defaults. SHOP_OWNER is always included. */
    private List<RoleSetupRequest> roleSetups;

    /** Master admin only: assign the AGENT (by agent id) who will manage this shop. */
    private Long vendorId;
}

