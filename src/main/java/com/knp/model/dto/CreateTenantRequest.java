package com.knp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String contactPersonName;

    private String contactPersonPhone;

    private String contactPersonEmail;

    private String contactPersonZaloId;
}

