package com.barbershop.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invoice_buyers")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class InvoiceBuyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_legal_name")
    private String buyerLegalName;

    @Column(name = "buyer_tax_code")
    private String buyerTaxCode;

    @Column(name = "buyer_address")
    private String buyerAddressLine;

    @Column(name = "buyer_phone_number")
    private String buyerPhoneNumber;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "buyer_bank_name")
    private String buyerBankName;

    @Column(name = "buyer_bank_account")
    private String buyerBankAccount;

    @Column(name = "buyer_id_number")
    private String buyerIdNumber;

    @Column(name = "is_visiting_guest")
    private boolean visitingGuest = false;
}

