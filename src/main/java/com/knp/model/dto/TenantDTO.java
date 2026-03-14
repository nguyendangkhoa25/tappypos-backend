package com.knp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

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
    private String dbName;

    private Boolean active;

    private LocalDate expirationDate;

    private Integer maxUsers;

    private List<String> features;

    private String subscriptionType;

    private String contactPersonName;

    private String contactPersonPhone;

    private String contactPersonEmail;

    private String contactPersonZaloId;

    private Long createdAt;

    private Long updatedAt;

    private Long activeAt;

    private String activeBy;

    private String createdBy;

    private String updatedBy;
}

