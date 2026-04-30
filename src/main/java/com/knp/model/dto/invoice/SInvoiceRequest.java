package com.knp.model.dto.invoice;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SInvoiceRequest {
    private List<ItemInfo> itemInfo;
    private List<Payment> payments;
    private BuyerRequest buyerInfo;
    private GeneralInvoiceInfo generalInvoiceInfo;
    private SummarizeInfo summarizeInfo;

    @Getter
    @Setter
    @Builder
    public static class SummarizeInfo {
        private String totalAmountWithTaxInWords;
        private BigDecimal sumOfTotalLineAmountWithoutTax;
        private BigDecimal totalAmountWithoutTax;
        private BigDecimal totalAmountWithTax;
        private BigDecimal totalAmountAfterDiscount;
    }

    @Getter
    @Setter
    @Builder
    public static class Payment {
        private String paymentMethodName;
    }

    @Getter
    @Setter
    public static class ItemInfo {
        private String itemName;
        private Long lineNumber;
        private Long itemId;
        private String itemCode;
        private BigDecimal itemTotalAmountWithTax;
        private BigDecimal unitPrice;
        private BigDecimal quantity;
        private BigDecimal itemTotalAmountAfterDiscount;
        private String unitName;
    }

    @Getter
    @Setter
    public static class GeneralInvoiceInfo {
        private Long invoiceIssuedDate;
        private String invoiceType;
        private String templateCode;
        private String transactionUuid;
        private String invoiceSeries;
        private String currencyCode;
        private Boolean paymentStatus;
        private String paymentTypeName;
        private Integer validation;
    }

    @Getter
    @Setter
    public static class BuyerRequest {
        private Long buyerId;
        private Long invoiceId;
        private Long customerId;
        private String buyerName;
        private String buyerLegalName;
        private String buyerTaxCode;
        private String buyerAddressLine;
        private String buyerPhoneNumber;
        private String buyerFaxNumber;
        private String buyerEmail;
        private String buyerBankName;
        private String buyerBankAccount;
        private String buyerIdNo;
        private String buyerIdType;
        private int buyerNotGetInvoice;
    }
}
