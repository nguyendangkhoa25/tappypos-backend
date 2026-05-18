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
public class InvoiceDTO {
    private Long id;
    private String direction;
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
    private String transactionUuid;
    private String codeOfTax;
    private LocalDateTime externalSyncAt;
    private String errorMessage;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Input invoice vendor fields
    private String supplierInvoiceNumber;
    private Long vendorId;
    private String vendorName;
    private String vendorTaxCode;
    private Long purchaseOrderId;

    // Orders and buyer info for display
    private List<OrderInfo> orders;
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
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal taxPercentage;
        private String status;
        private List<OrderItemInfo> orderItems;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemInfo {
        private Long id;
        private Long orderId;
        private String orderCustomerName;
        private String orderCustomerPhone;
        private String orderCustomerEmail;
        private Long productId;
        private String productName;
        private String serviceName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private BigDecimal taxPercentage;
        private BigDecimal taxAmount;
        private String status;
        private Long assignedEmployeeId;
        private String assignedEmployeeName;
        private String employeeName;
        private LocalDateTime completedAt;
        private Integer ordinalNumber;
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
        private String email;
        private String identityNumber;
        private String companyName;
        private String companyTaxCode;
        private String fax;
        private String address;
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


