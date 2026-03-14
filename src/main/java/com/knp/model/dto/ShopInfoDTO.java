package com.knp.model.dto;

import lombok.*;

import java.time.LocalDateTime;

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
}

