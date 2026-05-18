package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tappy.pos.model.enums.ShopType;
import java.time.LocalDate;
import java.util.List;

/**
 * UpdateTenantRequest - Request DTO for updating a tenant
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantRequest {

    private String name;

    private String dbName;


    private LocalDate expirationDate;

    private Integer maxUsers;

    private List<String> features;

    private String subscriptionType;

    private String contactPersonName;

    private String contactPersonPhone;

    private String contactPersonEmail;

    private String contactPersonZaloId;
    private ShopType shopType;

    /** Master admin only: reassign the AGENT who manages this shop (null = unassign). */
    private Long vendorId;
}

