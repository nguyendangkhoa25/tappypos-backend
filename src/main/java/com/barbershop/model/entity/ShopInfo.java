package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "shop_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopInfo extends BaseEntity {

    @NotBlank(message = "Shop name is required")
    @Column(nullable = false)
    private String shopName;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String companyName;

    // Default tax value in percentage (0-100)
    @Column(nullable = false)
    private Double defaultTaxRate = 0.0;

    // E-Invoice Credentials
    @Column(length = 100)
    private String eInvoiceUsername;

    @Column(length = 500)
    private String eInvoicePassword;

    @Column(length = 500)
    private String eInvoiceKey;

    // Phone number
    @Column(length = 20)
    private String phone;

    // Email
    @Column(length = 100)
    private String email;

    // Tax ID / Company registration number
    @Column(length = 150)
    private String taxCode;

    @Column(length = 150)
    private String invoiceVendor;

    // Website
    @Column(length = 200)
    private String website;


}

