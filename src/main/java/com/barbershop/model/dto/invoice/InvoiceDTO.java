package com.barbershop.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private String invoiceSeries;
    private LocalDateTime issuedDate;
    private BigDecimal totalAmountWithoutTax;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxPercentage;
    private String status;
    private String paymentType;
    private String invoiceType;
    private String currencyCode;
    private String externalInvoiceId;
    private LocalDateTime externalSyncAt;
    private String errorMessage;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Order and Customer info for display
    private OrderInfo order;
    private BuyerInfo buyer;
    private List<InvoiceItemDTO> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderInfo {
        private Long id;
        private String invoiceNumber;
        private CustomerInfo customer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerInfo {
        private Long id;
        private String name;
        private String phone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuyerInfo {
        private Long id;
        private Long customerId;
        private String buyerName;
        private String buyerLegalName;
        private String buyerTaxCode;
        private String buyerAddressLine;
        private String buyerPhoneNumber;
        private String buyerEmail;
        private String buyerBankName;
        private String buyerBankAccount;
        private String buyerIdNumber;
        private boolean visitingGuest;
    }
}


