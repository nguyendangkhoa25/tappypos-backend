package com.barbershop.model.dto;

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
    private String taxCode;
    private String website;
    private String invoiceVendor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

