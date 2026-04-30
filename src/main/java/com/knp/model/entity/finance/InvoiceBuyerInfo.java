package com.knp.model.entity.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceBuyerInfo {

    @Column(name = "buyer_name", length = 200)
    private String buyerName;

    @Column(name = "buyer_legal_name", length = 200)
    private String buyerLegalName;

    @Column(name = "buyer_tax_code", length = 50)
    private String buyerTaxCode;

    @Column(name = "buyer_address_line", length = 500)
    private String buyerAddressLine;

    @Column(name = "buyer_phone_number", length = 20)
    private String buyerPhoneNumber;

    @Column(name = "buyer_email", length = 200)
    private String buyerEmail;

    @Column(name = "buyer_bank_name", length = 200)
    private String buyerBankName;

    @Column(name = "buyer_bank_account", length = 50)
    private String buyerBankAccount;

    @Column(name = "buyer_id_number", length = 50)
    private String buyerIdNumber;

    @Builder.Default
    @Column(name = "visiting_guest")
    private boolean visitingGuest = false;

    @Column(name = "customer_id")
    private Long customerId;
}
