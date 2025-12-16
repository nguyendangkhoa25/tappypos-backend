package com.barbershop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TenantDTO - Data Transfer Object for Tenant
 * Used for API responses to hide sensitive information
 * (e.g., database passwords are not included in responses)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDTO {

    private Long id;

    private String tenantId;

    private String name;

    private Boolean active;
}

