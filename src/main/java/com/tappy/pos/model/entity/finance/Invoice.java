package com.tappy.pos.model.entity.finance;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.TenantAwareEntity;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.entity.order.Order;
import com.tappy.pos.model.enums.InvoiceDirection;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Invoice extends TenantAwareEntity {

    @Builder.Default
    @Column(name = "direction", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceDirection direction = InvoiceDirection.OUTPUT;

    @Column(name = "invoice_number", unique = true, length = 30)
    private String invoiceNumber;

    @Column(name = "invoice_series", length = 20)
    private String invoiceSeries;

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "total_amount_without_tax", precision = 15, scale = 2)
    private BigDecimal totalAmountWithoutTax;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "invoice_type", length = 50)
    private String invoiceType;

    @Builder.Default
    @Column(name = "currency_code", length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'VND'")
    private String currencyCode = "VND";

    @Column(name = "external_invoice_id", length = 100)
    private String externalInvoiceId;

    @Column(name = "transaction_uuid", length = 100)
    private String transactionUuid;

    @Column(name = "code_of_tax", length = 100)
    private String codeOfTax;

    @Column(name = "external_sync_at")
    private LocalDateTime externalSyncAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ── Input invoice vendor fields ───────────────────────────────────────────

    @Column(name = "supplier_invoice_number", length = 50)
    private String supplierInvoiceNumber;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "vendor_tax_code", length = 50)
    private String vendorTaxCode;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Embedded
    private InvoiceBuyerInfo buyerInfo;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    // Orders linked to this invoice via order.invoice FK
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    public enum InvoiceStatus {
        DRAFT,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public void issue() {
        this.status = InvoiceStatus.COMPLETED;
        this.issuedDate = LocalDateTime.now();
    }

    public void cancel() {
        this.status = InvoiceStatus.CANCELLED;
    }

    public void markFailed(String errorMessage) {
        this.status = InvoiceStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
