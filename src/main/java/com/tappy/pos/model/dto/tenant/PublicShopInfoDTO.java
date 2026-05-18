package com.tappy.pos.model.dto.tenant;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Public Shop Info DTO - Safe for public API exposure
 * Excludes sensitive E-Invoice credentials
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicShopInfoDTO {
    private Long id;
    private String shopName;
    private String address;
    private String companyName;
    private Double defaultTaxRate;
    private String phone;
    private String email;
    private String supplierTaxCode;
    private String website;
    private String cashDenominations;
    private String posMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

