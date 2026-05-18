package com.tappy.pos.model.dto.tenant;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopInfoDTO {
    private Long id;
    private String shopName;
    private String address;
    private String companyName;
    private Double defaultTaxRate;
    private Boolean taxAutoApply;
    private Map<String, Double> taxRateByProductType;
    private String eInvoiceUsername;
    private String eInvoicePassword;
    private String eInvoiceKey;
    private String phone;
    private String email;
    private String supplierTaxCode;
    private String website;
    private String cashDenominations;
    private String invoiceVendor;
    private String templateCode;
    private String invoiceSeries;
    private String invoiceSystem;
    private String posMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Pawn settings
    private BigDecimal pawnInterestRate;
    private Integer pawnInterestType;
    private Integer pawnDueDate;
    private Boolean excludeVisibleItem;
    private String pawnCategoryConfig;
    private String pawnDenominations;

    private String priceBoardCode;
    private String shopLocations;
}

