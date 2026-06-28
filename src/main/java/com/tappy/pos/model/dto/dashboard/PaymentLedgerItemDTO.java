package com.tappy.pos.model.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One row in the master cross-tenant payment ledger. {@code tenantName} is resolved from the
 * tenants master table (the payment row only carries {@code tenantId}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLedgerItemDTO {
    private Long id;
    private String tenantId;
    private String tenantName;
    private String provider;
    private String planCode;
    private String billingCycle;
    private long amount;
    private String status;
    private String txnRef;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
