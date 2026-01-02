package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotBlank(message = "Invoice number is required")
    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "invoice_series")
    private String invoiceSeries;

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_amount_without_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountWithoutTax;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "tax_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(name = "invoice_type")
    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType;

    @Column(name = "currency_code")
    private String currencyCode = "VND";

    @Column(name = "external_invoice_id")
    private String externalInvoiceId;

    @Column(name = "transaction_uuid")
    private String transactionUuid;

    @Column(name = "external_sync_at")
    private LocalDateTime externalSyncAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private InvoiceBuyer buyer;

    public enum InvoiceStatus {
        DRAFT,
        COMPLETED,
        FAILED,
        CANCELLED,
    }

    public enum InvoiceType {
        RETAIL("Bán lẻ", "RETAIL"),
        REFUND("Hoàn lại", "REFUND"),
        OTHER("Khác", "OTHER");

        public final String label;
        public final String code;

        InvoiceType(String label, String code) {
            this.label = label;
            this.code = code;
        }
    }

    public enum PaymentType {
        CASH("Tiền mặt", "CASH"),
        TRANSFER("Chuyển khoản", "TRANSFER"),
        CARD("Thẻ", "CARD");

        public final String label;
        public final String code;

        PaymentType(String label, String code) {
            this.label = label;
            this.code = code;
        }
    }

    public void syncWithExternal(String externalId) {
        this.externalInvoiceId = externalId;
        this.externalSyncAt = LocalDateTime.now();
    }
}

