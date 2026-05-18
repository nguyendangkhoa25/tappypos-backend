package com.tappy.pos.model.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInputInvoiceRequest {

    /** Loại hóa đơn: VAT_INVOICE | SALES_INVOICE | RECEIPT | OTHER */
    private String invoiceType;

    /** Ngày trên hóa đơn của nhà cung cấp */
    private LocalDateTime invoiceDate;

    /** Số hóa đơn của nhà cung cấp */
    private String supplierInvoiceNumber;

    /** FK to vendors table (optional) */
    private Long vendorId;

    /** Free-text vendor name (used when vendorId is absent) */
    private String vendorName;

    private String vendorTaxCode;

    /** Optional link to a purchase order */
    private Long purchaseOrderId;

    private String paymentType;

    @Builder.Default
    private String currencyCode = "VND";

    private BigDecimal taxPercentage;

    private String notes;

    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {
        private String itemName;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxPercentage;
        private BigDecimal discount;
    }
}
