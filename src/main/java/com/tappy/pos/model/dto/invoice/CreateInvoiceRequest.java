package com.tappy.pos.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {
    private List<Long> orderIds;
    private String paymentType;
    private String invoiceType;
    private String invoiceSeries;
    private BigDecimal taxPercentage;
    private String currencyCode;
    private String notes;
    private BuyerInfo buyerInfo;
    private List<InvoiceItemInput> items;
    // Order items (from frontend)
    private List<OrderItemInput> orderItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuyerInfo {
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceItemInput {
        private Long orderItemId;
        private String serviceName;
        private String serviceCode;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal quantity;
        private BigDecimal discount;
        private BigDecimal taxPercentage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemInput {
        private Long id;
        private Integer ordinalNumber;
        private String serviceName;
        private String productName;
        private BigDecimal price;
        private BigDecimal quantity;
        private BigDecimal totalPrice;
    }
}