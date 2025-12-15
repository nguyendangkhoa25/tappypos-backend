package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @NotBlank(message = "Invoice number is required")
    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "external_invoice_id")
    private String externalInvoiceId;

    @Column(name = "external_sync_at")
    private LocalDateTime externalSyncAt;

    @Column(length = 1000)
    private String notes;

    public enum InvoiceStatus {
        DRAFT,
        ISSUED,
        PAID,
        CANCELLED,
        SYNCED_WITH_EXTERNAL
    }

    public void syncWithExternal(String externalId) {
        this.externalInvoiceId = externalId;
        this.status = InvoiceStatus.SYNCED_WITH_EXTERNAL;
        this.externalSyncAt = LocalDateTime.now();
    }
}

